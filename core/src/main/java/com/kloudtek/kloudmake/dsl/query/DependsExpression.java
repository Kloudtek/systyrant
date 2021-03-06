/*
 * Copyright (c) 2015. Kelewan Technologies Ltd
 */

package com.kloudtek.kloudmake.dsl.query;

import com.kloudtek.kloudmake.KMContextImpl;
import com.kloudtek.kloudmake.Resource;
import com.kloudtek.kloudmake.dsl.KloudmakeLangParser;
import com.kloudtek.kloudmake.exception.InvalidQueryException;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: ymenager
 * Date: 11/03/2013
 * Time: 23:36
 * To change this template use File | Settings | File Templates.
 */
public class DependsExpression extends Expression {
    boolean recurse;
    private ArrayList<Resource> resources = new ArrayList<>();

    public DependsExpression(KloudmakeLangParser.QueryDependsMatchContext depCtx, String query, KMContextImpl context, Resource baseResource) throws InvalidQueryException {
        if (depCtx.exp != null) {
            Expression expression = Expression.create(depCtx.exp, query, context, baseResource);
            for (Resource resource : context.getResourceManager()) {
                if (expression.matches(context, resource)) {
                    resources.add(resource);
                }
            }
        } else {
            Resource resource = context.currentResource();
            if (resource == null) {
                throw new InvalidQueryException("'childof' has no parameters specified and no resource is in scope: " + query);
            }
            resources.add(resource);
        }
        recurse = depCtx.s != null;
    }

    @Override
    public boolean matches(KMContextImpl context, Resource resource) {
        // todo fail is dependency resolution not done
        if (recurse) {
            return false;
        } else {
            for (Resource r : resources) {
                if (resource.getDependencies().contains(r)) {
                    return true;
                }
            }
            return false;
        }
    }
}
