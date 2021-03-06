/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.scheduler.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.ConceptServiceImpl;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;

/**
 * A utility class for updating concept words in a scheduled task.
 */
public class ConceptIndexUpdateTask extends AbstractTask {
	
	private Log log = LogFactory.getLog(ConceptIndexUpdateTask.class);
	
	private boolean shouldExecute = true;
	
	/**
	 * @see org.openmrs.scheduler.tasks.AbstractTask#execute()
	 */
	@Override
	public void execute() {
		if (!isExecuting) {
			isExecuting = true;
			shouldExecute = true;
			
			if (log.isDebugEnabled())
				log.debug("Updating concept words ... ");
			try {
				ConceptService cs = Context.getConceptService();
				Concept currentConcept = cs.getNextConcept(new Concept(0)); // assumes that all conceptIds are positive
				int counter = 0;
				while (currentConcept != null && shouldExecute) {
					if (log.isDebugEnabled())
						log.debug("updateConceptWords() : current concept: " + currentConcept);
					cs.updateConceptIndex(currentConcept);
					
					// keep memory consumption low
					if (counter++ > 200) {
						Context.clearSession();
						counter = 0;
					}
					
					currentConcept = cs.getNextConcept(currentConcept);
				}
			}
			catch (APIException e) {
				log.error("ConceptWordUpdateTask failed, because:", e);
				throw e;
			}
			finally {
				isExecuting = false;
				shouldExecute = false;
				SchedulerService ss = Context.getSchedulerService();
				TaskDefinition conceptWordUpdateTaskDef = ss.getTaskByName(ConceptServiceImpl.CONCEPT_WORD_UPDATE_TASK_NAME);
				conceptWordUpdateTaskDef.setStarted(false);
				//This was need when upgrading to version1.8
				//Otherwise it will always return false
				if (conceptWordUpdateTaskDef.getStartOnStartup())
					conceptWordUpdateTaskDef.setStartOnStartup(false);
				ss.saveTask(conceptWordUpdateTaskDef);
				log.debug("Task set to stopped.");
			}
		}
	}
	
	/**
	 * @see org.openmrs.scheduler.Task#initialize(org.openmrs.scheduler.TaskDefinition)
	 */
	@Override
	public void initialize(TaskDefinition config) {
		// do nothing
	}
	
	/**
	 * @see org.openmrs.scheduler.Task#shutdown()
	 */
	@Override
	public void shutdown() {
		shouldExecute = false;
	}
	
}
