/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant;

import com.kloudtek.systyrant.exception.*;
import com.kloudtek.systyrant.host.Host;
import com.kloudtek.systyrant.resource.*;
import org.apache.commons.beanutils.ConvertUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.kloudtek.util.StringUtils.isNotEmpty;

public class Resource {
    public static final String SUBSCRIBE = "subscribe";
    public static final String NOTIFY = "notify";
    private static final Logger logger = LoggerFactory.getLogger(Resource.class);
    private Map<String, String> attributes = new HashMap<>();
    private transient STContext context;
    private ResourceDefinition definition;
    private Resource parent;
    private boolean executable;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final HashSet<Resource> childrens = new HashSet<>();
    private List<Action> prepareActions = new ArrayList<>();
    private List<Action> execActions = new ArrayList<>();
    private List<Action> postChildrenExecActions = new ArrayList<>();
    private List<Action> cleanupActions = new ArrayList<>();
    private State state;
    private Host hostOverride;
    /**
     * This contains the results of any successful verification (meaning nothing has changed). A global verification
     * will be stored as an empty string, and a specific verification will be stored as it's name.
     */
    private final HashSet<String> verification = new HashSet<>();
    private final HashSet<Object> javaImpls = new HashSet<>();
    private final HashSet<NotificationHandler> notificationHandlers = new HashSet<>();
    final HashSet<Resource> dependencies = new HashSet<>();
    HashSet<Resource> indirectDependencies;
    final HashSet<Resource> dependents = new HashSet<>();
    final HashMap<String, List<Resource>> requires = new HashMap<>();

    public Resource(STContext context, ResourceDefinition definition, String id, String uid, Resource parent) {
        this.context = context;
        this.definition = definition;
        this.parent = parent;
        attributes.put("id", id);
        attributes.put("uid", uid);
        if (parent != null) {
            parent.childrens.add(this);
        }
        reset();
    }

    public void reset() {
        state = State.NEW;
        verification.clear();
        indirectDependencies = null;
        dependencies.clear();
    }

    // ----------------------------------------------------------------------
    // Dependency Management
    // ----------------------------------------------------------------------

    public Set<Resource> getChildrens() {
        return Collections.unmodifiableSet(childrens);
    }

    public Set<Resource> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public ResourceDependency addDependencies(Collection<Resource> resources) {
        return addDependencies(resources, false);
    }

    public Lock rlock() {
        return lock.readLock();
    }

    public Lock wlock() {
        return lock.writeLock();
    }

    public ResourceDependency addDependencies(Collection<Resource> resources, boolean optional) {
        if (resources.contains(this)) {
            throw new IllegalArgumentException("Added dependencies contain self: " + resources);
        }
        ResourceDependency depRef = new OneToManyResourceDependency(this, resources, optional);
        context.getResourceManager().addDependency(depRef);
        return depRef;
    }

    public ResourceDependency addDependency(Resource resource) {
        return addDependency(resource, false);
    }

    public ResourceDependency addDependency(String ref) throws InvalidRefException {
        return addDependency(ref, false);
    }

    public ResourceDependency addDependency(Resource resource, boolean optional) {
        if (resource.equals(this)) {
            throw new IllegalArgumentException("Added dependency on self: " + resource);
        }
        ResourceDependency depRef = new OneToManyResourceDependency(this, resource, optional);
        context.getResourceManager().addDependency(depRef);
        return depRef;
    }

    public ResourceDependency addDependency(String ref, boolean optional) throws InvalidRefException {
        ResourceDependency dep = new OneToManyResourceDependency(this, ref, optional);
        context.getResourceManager().addDependency(dep);
        return dep;
    }

    public Resource getParent() {
        return parent;
    }

    public Set<String> getRequires() {
        return Collections.unmodifiableSet(requires.keySet());
    }

    @Nullable
    public List<Resource> getResolvedRequires(String expr) {
        return requires.get(expr);
    }

    public void addRequires(String requiresExpr) {
        if (requires.get(requiresExpr) == null) {
            requires.put(requiresExpr, null);
        }
    }

    public void removeRequires(String requiresExpr) {
        requires.remove(requiresExpr);
    }

    public void assignedResolvedRequires(String requiresExpr, List<Resource> resources) {
        requires.put(requiresExpr, resources);
    }

    public Set<Resource> getIndirectDependencies() {
        return indirectDependencies;
    }

    public void prepareForExecution(STContext context) throws InvalidAttributeException {
        // TODO this needs to move to lifecycle executor
        indirectDependencies = new HashSet<>(dependencies);
        for (Resource dep : dependencies) {
            assert dep.getIndirectDependencies() != null;
            indirectDependencies.addAll(dep.getDependencies());
        }
        String subscribe = get(SUBSCRIBE);
        if (isNotEmpty(subscribe)) {
            try {
                for (Resource res : context.findResources(subscribe)) {
                    res.addAutoNotification(this);
                }
            } catch (InvalidQueryException e) {
                throw new InvalidAttributeException("Invalid query specified in " + getUid() + " subscribe attribute: " + subscribe);
            }
            removeAttribute(SUBSCRIBE);
        }
        String notify = get(NOTIFY);
        if (isNotEmpty(notify)) {
            try {
                addAutoNotifications(context.findResources(notify));
            } catch (InvalidQueryException e) {
                throw new InvalidAttributeException("Invalid query specified in " + getUid() + " notify attribute: " + notify);
            }
            removeAttribute(NOTIFY);
        }
    }

    // ----------------------------------------------------------------------
    // Notifications
    // ----------------------------------------------------------------------

    /**
     * Add a notification handler to this resource. This can only be done before the {@link Stage#EXECUTE} stage.
     *
     * @param notificationHandler Handler to add to this resource.
     * @throws InvalidStageException If attempted to add the handler at EXECUTE or later stage.
     */
    public void addNotificationHandler(NotificationHandler notificationHandler) throws InvalidStageException {
        synchronized (notificationHandlers) {
            switch (state) {
                case PREPARED:
                    throw new InvalidStageException("Notification handlers cannot be added after the ");
            }
            notificationHandlers.add(notificationHandler);
        }
    }

    // ----------------------------------------------------------------------
    // Actions
    // ----------------------------------------------------------------------

    public void addAction(@NotNull Action action) {
        switch (action.getType()) {
            case PREPARE:
                prepareActions.add(action);
                break;
            case EXECUTE:
            case SYNC:
                execActions.add(action);
                break;
            case POSTCHILDREN_EXECUTE:
            case POSTCHILDREN_SYNC:
                postChildrenExecActions.add(action);
                break;
            case CLEANUP:
                cleanupActions.add(action);
        }
    }

    public synchronized void sortActions() {
        Collections.sort(prepareActions);
        Collections.sort(execActions);
        Collections.sort(postChildrenExecActions);
        Collections.sort(cleanupActions);
    }

    // ----------------------------------------------------------------------
    // Meta-Data retrieval
    // ----------------------------------------------------------------------

    public synchronized void addJavaImpl(Object obj) {
        javaImpls.add(obj);
    }

    public synchronized <X> X getJavaImpl(Class<X> clazz) {
        for (Object javaImpl : javaImpls) {
            if (javaImpl.getClass().isAssignableFrom(clazz)) {
                return clazz.cast(javaImpl);
            }
        }
        return null;
    }

    public Host getHost() {
        if (hostOverride != null) {
            return hostOverride;
        } else if (parent != null) {
            return parent.getHost();
        } else {
            return context.getHost();
        }
    }

    public synchronized Host getHostOverride() {
        return hostOverride;
    }

    public synchronized void setHostOverride(Host hostOverride) throws STRuntimeException {
        if (this.hostOverride != null && this.hostOverride != hostOverride) {
            this.hostOverride.stop();
        }
        this.hostOverride = hostOverride;
        if (hostOverride != null) {
            context.inject(hostOverride);
            synchronized (hostOverride) {
                if (!hostOverride.isStarted()) {
                    hostOverride.start();
                }
            }
        }
    }

    public ResourceDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        return definition.getName();
    }

    public String getPkg() {
        return definition.getPkg();
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    // ------------------------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------------------------

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, Object> attributes) throws InvalidAttributeException {
        logger.debug("Setting {}'s attributes to {}", definition.getFQName(), attributes);
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets the value for one of the resource's properties.
     *
     * @param key      Attribute key.
     * @param valueObj Attribute value.
     * @return This resource.
     */
    public Resource set(@NotNull String key, @Nullable Object valueObj) throws InvalidAttributeException {
        key = key.trim().toLowerCase();
        if (key.equalsIgnoreCase("id") || key.equalsIgnoreCase("uid")) {
            throw new InvalidAttributeException("attribute id cannot be modified");
        }
        String value = ConvertUtils.convert(valueObj);
        logger.debug("Setting {}'s attribute {} to {}", definition.getFQName(), key, value);
        attributes.put(key, value);
        return this;
    }

    public String get(@NotNull String key) {
        return attributes.get(key.toLowerCase().toLowerCase());
    }

    public void removeAttribute(@NotNull String key) {
        if (key.equalsIgnoreCase("id")) {
            throw new IllegalArgumentException("attribute id cannot be removed");
        }
        attributes.remove(key.toLowerCase());
    }

    public String getId() {
        return get("id");
    }

    void setId(String value) {
        attributes.put("id", value);
        logger.debug("setting id {}", value);
    }

    public String getUid() {
        return get("uid");
    }

    public void setUid(@Nullable String uid) throws InvalidAttributeException {
        set("uid", uid);
    }

    public String generateUid() {
        StringBuilder tmp = new StringBuilder();
        Resource p = getParent();
        if (p != null) {
            tmp.append(p.generateUid()).append('.');
        }
        return tmp.append(getId()).toString();
    }

    public void addVerification(@NotNull String name) {
        synchronized (verification) {
            verification.add(name.toLowerCase());
        }
    }

    public boolean containsVerification(String verificationString) {
        synchronized (verification) {
            return verification.contains(verificationString.toLowerCase());
        }
    }

    public Set<String> getVerification() {
        synchronized (verification) {
            return Collections.unmodifiableSet(verification);
        }
    }

    public void addAutoNotification(Resource resource) {
        context.addAutoNotification(new AutoNotify(this, resource, null));
    }

    public void addAutoNotifications(Collection<Resource> resources) {
        for (Resource resource : resources) {
            addAutoNotification(resource);
        }
    }

    // ----------------------------------------------------------------------
    // Utility functions
    // ----------------------------------------------------------------------

    public String toString() {
        return definition.getFQName().toString() + ":" + getUid();
    }

    public Logger logger() {
        return logger;
    }

    public STContext context() {
        return context;
    }

    public Host host() {
        return getHost();
    }

    public FQName getType() {
        return definition.getFQName();
    }

    // ----------------------------------------------------------------------
    // Processing Methods
    // ----------------------------------------------------------------------

    public void executeActions(Stage stage, boolean postChildren) throws STRuntimeException {
        List<Action> list;
        switch (stage) {
            case PREPARE:
                list = prepareActions;
                break;
            case EXECUTE:
                if (postChildren) {
                    list = postChildrenExecActions;
                } else {
                    list = execActions;
                }
                break;
            case CLEANUP:
                list = cleanupActions;
                break;
            default:
                throw new STRuntimeException("BUG: Invalid stage " + stage);
        }
        HashSet<String> supportedAlternatives = new HashSet<>();
        HashSet<String> requiredAlternatives = new HashSet<>();
        for (Action action : list) {
            String alternative = action.getAlternative();
            if (alternative != null) {
                requiredAlternatives.add(alternative);
            }
            if (action.supports(context, this)) {
                if (alternative != null) {
                    supportedAlternatives.add(alternative);
                }
                if (action.checkExecutionRequired(context, this)) {
                    action.execute(context, this);
                }
            }
        }
        for (String requiredAlternative : requiredAlternatives) {
            if (!supportedAlternatives.contains(requiredAlternative)) {
                StringBuilder msg = new StringBuilder("Unable to find ").append(requiredAlternative);
                if (!requiredAlternative.isEmpty()) {
                    msg.append(' ');
                }
                msg.append("alternative for ").append(getType());
                throw new MissingAlternativeException(msg.toString());
            }
        }
    }

    public void handleNotification(Notification notification) throws STRuntimeException {
        synchronized (notificationHandlers) {
            for (NotificationHandler handler : notificationHandlers) {
                if (handler.isSameCategory(handler.getCategory()) && (!handler.isOnlyIfAfter() || state.ordinal() >= State.EXECUTED.ordinal())) {
                    handler.handleNotification(notification);
                }
            }
        }
    }

    public boolean isReOrderRequiredForNotification(@NotNull String category) {
        for (NotificationHandler notificationHandler : notificationHandlers) {
            if (notificationHandler.getCategory().equalsIgnoreCase(category) && notificationHandler.isReorder()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAggregationSupportedForNotification(@NotNull String category) {
        boolean match = false;
        for (NotificationHandler notificationHandler : notificationHandlers) {
            if (notificationHandler.getCategory().equalsIgnoreCase(category)) {
                if (notificationHandler.isAggregate()) {
                    // Notification handler supports aggregation
                    match = true;
                } else if (match) {
                    // There's a notification handler on that category that doesn't support aggregation
                    return false;
                }
            }
        }
        return match;
    }

    public enum State {
        NEW, PREPARED, EXECUTED, CLEANEDUP, SKIP, FAILED
    }
}