/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.zkoss.ganttz;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.data.TaskContainer;
import org.zkoss.ganttz.data.TaskContainer.IExpandListener;
import org.zkoss.zk.ui.ext.AfterCompose;

/**
 * This class contains the information of a task container. It can be modified
 * and notifies of the changes to the interested parties. <br/>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class TaskContainerComponent extends TaskComponent implements
        AfterCompose {

    public static TaskContainerComponent asTask(Task taskContainerBean,
            TaskList taskList) {
        return new TaskContainerComponent((TaskContainer) taskContainerBean,
                taskList);
    }

    private List<TaskComponent> subtaskComponents = new ArrayList<TaskComponent>();
    private final TaskList taskList;

    private transient IExpandListener expandListener;

    public TaskContainerComponent(final TaskContainer taskContainer,
            final TaskList taskList) {
        super(taskContainer, taskList.getDisabilityConfiguration());
        if (!taskContainer.isContainer()) {
            throw new IllegalArgumentException();
        }
        this.taskList = taskList;
        this.expandListener = new IExpandListener() {

            @Override
            public void expandStateChanged(boolean isNowExpanded) {
                taskList.reload(true);
                updateClass();
            }
        };
        taskContainer.addExpandListener(expandListener);
        for (Task task : taskContainer.getTasks()) {
            getCurrentComponents().add(createChild(task));
        }
    }

    private TaskComponent createChild(Task task) {
        return TaskComponent.asTaskComponent(task, this.taskList, false);
    }

    @Override
    protected void publishDescendants(Map<Task, TaskComponent> resultAccumulated) {
        for (TaskComponent taskComponent : getCurrentComponents()) {
            taskComponent.publishTaskComponents(resultAccumulated);
        }
    }

    @Override
    protected void remove() {
        if (isExpanded()) {
            for (TaskComponent subtaskComponent : getCurrentComponents()) {
                subtaskComponent.remove();
            }
        }
        super.remove();
    }

    private List<TaskComponent> getCurrentComponents() {
        ListIterator<TaskComponent> listIterator = subtaskComponents
                .listIterator();
        // one of the subtask components created previously could have been
        // removed, so we only return the valid ones
        while (listIterator.hasNext()) {
            if (!getTaskContainer().contains(listIterator.next().getTask())) {
                listIterator.remove();
            }
        }
        return subtaskComponents;
    }

    public boolean isExpanded() {
        return getTaskContainer().isExpanded();
    }

    private TaskContainer getTaskContainer() {
        return (TaskContainer) getTask();
    }

    @Override
    protected String calculateClass() {
        return super.calculateClass() + " "
                + (getTaskContainer().isExpanded() ? "expanded" : "closed");
    }
}