/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
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

package org.navalplanner.ws.resources.impl;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang.StringUtils;
import org.hibernate.NonUniqueResultException;
import org.navalplanner.business.common.IntegrationEntity;
import org.navalplanner.business.common.daos.IEntitySequenceDAO;
import org.navalplanner.business.common.daos.IIntegrationEntityDAO;
import org.navalplanner.business.common.entities.EntityNameEnum;
import org.navalplanner.business.common.entities.EntitySequence;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.costcategories.entities.ResourcesCostCategoryAssignment;
import org.navalplanner.business.resources.daos.IMachineDAO;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.daos.IWorkerDAO;
import org.navalplanner.business.resources.entities.CriterionSatisfaction;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsListDTO;
import org.navalplanner.ws.common.impl.GenericRESTService;
import org.navalplanner.ws.common.impl.RecoverableErrorException;
import org.navalplanner.ws.resources.api.IResourceService;
import org.navalplanner.ws.resources.api.ResourceDTO;
import org.navalplanner.ws.resources.api.ResourceListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST-based implementation of <code>IResourceService</code>.
 *
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
@Path("/resources/")
@Produces("application/xml")
@Service("resourceServiceREST")
public class ResourceServiceREST
    extends GenericRESTService<Resource, ResourceDTO>
    implements IResourceService {

    @Autowired
    private IResourceDAO resourceDAO;

    @Autowired
    private IEntitySequenceDAO entitySequenceDAO;

    @Override
    @POST
    @Consumes("application/xml")
    public InstanceConstraintViolationsListDTO addResources(
        ResourceListDTO resources) {

        return save(resources.resources);

    }

    @Override
    @Transactional
    protected Resource toEntity(ResourceDTO entityDTO)
        throws ValidationException, RecoverableErrorException {

        Resource resource = ResourceConverter.toEntity(entityDTO);

        // set the generated code to its criterion satisfaction, its resources
        // cost categories assignment and its resource calendar
        generateCodes(resource);

        return resource;

    }

    @Override
    protected ResourceDTO toDTO(Resource entity) {
        return ResourceConverter.toDTO(entity);
    }

    @Override
    protected IIntegrationEntityDAO<Resource> getIntegrationEntityDAO() {
        return resourceDAO;
    }

    @Override
    protected void updateEntity(Resource entity, ResourceDTO entityDTO)
        throws ValidationException, RecoverableErrorException {

        ResourceConverter.updateResource(entity, entityDTO);
        // set the generated code to its criterion satisfaction, its resources
        // cost categories assignment and its resource calendar
        generateCodes(entity);
    }

    @Autowired
    private IWorkerDAO workerDAO;

    @Autowired
    private IMachineDAO machineDAO;

    @Override
    @GET
    @Transactional(readOnly = true)
    public ResourceListDTO getResources() {
        return new ResourceListDTO(findAll());
    }

    @Override
    protected List<ResourceDTO> findAll() {
        List<Resource> result = new ArrayList<Resource>();
        result.addAll(workerDAO.getWorkers());
        result.addAll(machineDAO.getAll());
        return toDTO(result);
    }

    private void generateCodes(Resource resource) {
        // set autogenerated code to CriterionSatisfaction
        for (CriterionSatisfaction satisfaction : resource
                .getAllSatisfactions()) {
            if (satisfaction.isNewObject()
                    && StringUtils.isBlank(satisfaction.getCode())) {
                setDefaultCode(EntityNameEnum.CRITERION_SATISFACTION,
                        satisfaction);
            }
        }

        // set autogenerated code to ResourcesCostCategoryAssignment
        for (ResourcesCostCategoryAssignment assignment : resource
                .getResourcesCostCategoryAssignments()) {
            if (assignment.isNewObject()
                    && StringUtils.isBlank(assignment.getCode())) {
                setDefaultCode(
                        EntityNameEnum.RESOURCE_COST_CATEGORY_ASSIGNMENT,
                        assignment);
            }
        }

        // set autogenerated code to ResourceCalendar
        if (resource.getCalendar().isNewObject()) {
            setDefaultCode(EntityNameEnum.RESOURCE_CALENDAR, resource
                    .getCalendar());
        }
        resource.getCalendar().generateCalendarExceptionCodes(
                getNumberOfDigitsCode(EntityNameEnum.RESOURCE_CALENDAR));

    }

    private void setDefaultCode(EntityNameEnum entityName,
            IntegrationEntity entity) throws ConcurrentModificationException {
        String code = entitySequenceDAO.getNextEntityCode(entityName);
        if (code == null) {
            throw new ConcurrentModificationException(
                    _("Could not get code, please try again later"));
        }
        entity.setCode(code);
        entity.setCodeAutogenerated(true);
    }

    private Integer getNumberOfDigitsCode(EntityNameEnum entityName) {
        int numberOfDigits = 5;
        try {
            EntitySequence entitySequence = entitySequenceDAO
                    .getActiveEntitySequence(entityName);
            numberOfDigits = entitySequence.getNumberOfDigits();
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NonUniqueResultException e) {
            throw new RuntimeException(e);
        }
        return numberOfDigits;
    }

}
