/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.kloudmake.dsl.statement;

import com.kloudtek.kloudmake.FQName;
import com.kloudtek.kloudmake.KMContextImpl;
import com.kloudtek.kloudmake.Resource;
import com.kloudtek.kloudmake.dsl.*;
import com.kloudtek.kloudmake.exception.STRuntimeException;
import org.jetbrains.annotations.Nullable;

import javax.script.ScriptException;
import java.util.*;

public class CreateResourceStatement extends Statement {
    private FQName type;
    private List<Instance> instances = new ArrayList<>();
    private KMContextImpl ctx;

    public CreateResourceStatement(KMContextImpl ctx, KloudmakeLangParser.CreateResourceContext createElementsContext) throws InvalidScriptException {
        this.ctx = ctx;
        type = new FQName(createElementsContext.type.getText());
        Map<String, Parameter> params = new LinkedHashMap<>();
        KloudmakeLangParser.CreateResourceParamsContext paramsCtx = createElementsContext.params;
        if (paramsCtx != null) {
            for (KloudmakeLangParser.ParameterAssignmentContext pctx : AntLRUtils.nullToEmpty(paramsCtx.parameterAssignment())) {
                if (pctx != null) {
                    String paramName = pctx.paramName.getText();
                    Parameter parameter = Parameter.create(pctx.staticOrDynamicValue());
                    params.put(paramName, parameter);
                }
            }
        }
        KloudmakeLangParser.CreateResourceStatementsContext resourceStatements = createElementsContext.createResourceStatements();
        if (resourceStatements != null) {
            KloudmakeLangParser.CreateResourceSingleInstanceContext singleResource = resourceStatements.createResourceSingleInstance();
            if (singleResource != null) {
                parseResource(singleResource.createResourceInstanceId(), singleResource.createResourceInstanceElements(), params);
            } else {
                List<KloudmakeLangParser.CreateResourceMultipleInstanceContext> multipleInstances = AntLRUtils.nullToEmpty(resourceStatements.createResourceMultipleInstance());
                for (KloudmakeLangParser.CreateResourceMultipleInstanceContext instance : multipleInstances) {
                    parseResource(instance.createResourceInstanceId(), instance.createResourceInstanceElements(), params);
                }
            }
        } else {
            instances.add(new Instance(null, null, params));
        }
    }

    private void parseResource(KloudmakeLangParser.CreateResourceInstanceIdContext resourceInstanceId,
                               List<KloudmakeLangParser.CreateResourceInstanceElementsContext> resourceInstanceElements, Map<String, Parameter> params) throws InvalidScriptException {
        String id = AntLRUtils.toString(resourceInstanceId != null ? resourceInstanceId.id : null);
        instances.add(new Instance(id, resourceInstanceElements, params));
    }

    public List<Instance> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    @Override
    public List<Resource> execute(DSLScript dslScript, Resource parent) throws ScriptException {
        try {
            ArrayList<Resource> resources = new ArrayList<>();
            for (Instance instance : instances) {
                String oldSource = ctx.getSourceUrl();
                ctx.setSourceUrl(dslScript.getSourceUrl());
                assert ctx.getSourceUrl() != null;
                try {
                    Resource resource = ctx.getResourceManager().createResource(type, instance.id, parent, dslScript.getImports());
                    Resource old = ctx.currentResource();
                    ctx.setCurrentResource(resource);
                    for (CreateAction action : instance.actions) {
                        action.execute(dslScript, resource);
                    }
                    resources.add(resource);
                    if (old != null) {
                        ctx.setCurrentResource(old);
                    }
                } finally {
                    if (oldSource != null) {
                        ctx.setSourceUrl(oldSource);
                    } else {
                        ctx.clearSourceUrl();
                    }
                }
            }
            return resources;
        } catch (STRuntimeException e) {
            throw new ScriptException(e);
        }
    }

    public FQName getType() {
        return type;
    }

    @Override
    public String toString() {
        return "createres{" + type + " : " + instances + "}";
    }

    public class Instance {
        private String id;
        private final List<CreateAction> actions = new ArrayList<>();

        public Instance(@Nullable String id,
                        @Nullable List<KloudmakeLangParser.CreateResourceInstanceElementsContext> resourceInstanceElements,
                        @Nullable Map<String, Parameter> params) throws InvalidScriptException {
            this.id = id;
            if (params != null) {
                for (Map.Entry<String, Parameter> entry : params.entrySet()) {
                    String key = entry.getKey();
                    Parameter value = entry.getValue();
                    if (key.equals("id")) {
                        assignId(value);
                    } else {
                        actions.add(new SetAttrAction(key, value));
                    }
                }
            }
            for (KloudmakeLangParser.CreateResourceInstanceElementsContext rsCtx : AntLRUtils.nullToEmpty(resourceInstanceElements)) {
                final CreateAction action;
                if (rsCtx.aspar != null) {
                    String paramName = rsCtx.aspar.paramName.getText().trim().toLowerCase();
                    Parameter parameter = Parameter.create(rsCtx.aspar.staticOrDynamicValue());
                    if (paramName.equals("id")) {
                        assignId(parameter);
                        continue;
                    } else {
                        action = new SetAttrAction(paramName, parameter);
                    }
                } else if (rsCtx.child != null) {
                    action = new CreateChildAction(rsCtx.child.createResource());
                } else if (rsCtx.asvar != null) {
                    action = new SetVarAction(rsCtx.asvar);
                } else {
                    throw new InvalidScriptException("BUG! Unknown statement " + rsCtx.getText());
                }
                actions.add(action);
            }
        }

        private void assignId(Parameter value) throws InvalidScriptException {
            if (value instanceof StaticParameter) {
                this.id = value.getRawValue();
            } else {
                throw new InvalidScriptException("id " + value.getRawValue() + " in " + type + "must be a static value");
            }
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "instance{" + id + "}";
        }

        public Parameter getAttrAssignment(String name) {
            for (CreateAction action : actions) {
                if (action instanceof SetAttrAction) {
                    SetAttrAction attrAction = (SetAttrAction) action;
                    if (attrAction.paramName.equals(name)) {
                        return attrAction.value;
                    }
                }
            }
            return null;
        }
    }

    public interface CreateAction {
        void execute(DSLScript dslScript, Resource resource) throws STRuntimeException, ScriptException;
    }

    public class SetAttrAction implements CreateAction {
        private String paramName;
        private Parameter value;

        public SetAttrAction(String paramName, Parameter value) {
            this.paramName = paramName;
            this.value = value;
        }

        @Override
        public void execute(DSLScript dslScript, Resource resource) throws STRuntimeException {
            resource.set(paramName, value.eval(ctx, resource));
        }
    }

    public class CreateChildAction implements CreateAction {
        private final CreateResourceStatement statement;

        public CreateChildAction(KloudmakeLangParser.CreateResourceContext createChildCtx) throws InvalidScriptException {
            statement = new CreateResourceStatement(ctx, createChildCtx);
        }

        @Override
        public void execute(DSLScript dslScript, Resource resource) throws STRuntimeException, ScriptException {
            statement.execute(dslScript, resource);
        }
    }

    public class SetVarAction implements CreateAction {
        private String name;
        private Parameter value;

        public SetVarAction(KloudmakeLangParser.AssignVariableStatementContext asvar) throws InvalidScriptException {
            name = asvar.var.getText();
            value = Parameter.create(asvar.val);
        }

        @Override
        public void execute(DSLScript dslScript, Resource resource) throws STRuntimeException, ScriptException {
            resource.setVar(name, value.eval(ctx, resource));
        }
    }
}
