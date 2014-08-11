/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.kloudmake;

import com.kloudtek.kloudmake.exception.STRuntimeException;

public interface Task extends Comparable<Task> {
    /**
     * Executes the action
     *
     * @param context  Context
     * @param resource Resource being executed
     * @throws STRuntimeException If an error occurs during execution
     */
    void execute(KMContextImpl context, Resource resource) throws STRuntimeException;

    /**
     * Checks if execution is required.
     *
     * @param context
     * @param resource
     * @return True if execution is required.
     */
    boolean checkExecutionRequired(KMContextImpl context, Resource resource) throws STRuntimeException;

    /**
     * Checks if this action supports running on the resource
     *
     * @param context  Context
     * @param resource Resource
     * @return True if the action supports running on the resource and it's environment.
     * @throws STRuntimeException If an error occurs
     * @see #getAlternative()
     */
    boolean supports(KMContextImpl context, Resource resource) throws STRuntimeException;

    /**
     * Get the order ranking (if two tasks are in the same stage, the higher order one will run first)
     *
     * @return
     */
    int getOrder();

    /**
     * Get at which stage this task will run
     *
     * @return Stage the action will run.
     */
    Stage getStage();

    /**
     * If this flag is true, the execution of this task will be delayed until all it's childrens have completed the stage
     *
     * @return
     */
    boolean isPostChildren();

    /**
     * Return the alternative id.
     * If any action on a resource has a non-null alternative value, this means that it will expect at least of the actions
     * of that id to be supported (meaning that {@link #supports(KMContextImpl, Resource)} has returned
     * true).
     *
     * @return Alternative id or null if not an alternative
     * @see #supports(KMContextImpl, Resource)
     */
    String getAlternative();
}