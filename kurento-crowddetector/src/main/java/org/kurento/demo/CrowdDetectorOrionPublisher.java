/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.demo;

import static com.google.common.collect.Lists.newArrayListWithCapacity;

import java.util.Collection;
import java.util.List;

import org.kurento.module.crowddetector.CrowdDetectorDirectionEvent;
import org.kurento.module.crowddetector.CrowdDetectorFluidityEvent;
import org.kurento.module.crowddetector.CrowdDetectorOccupancyEvent;
import org.kurento.module.crowddetector.RegionOfInterest;
import org.kurento.orion.OrionConnector;
import org.kurento.orion.entities.OrionAttribute;
import org.kurento.orion.entities.OrionContextElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Ivan Gracia (igracia@kurento.com)
 *
 */
public class CrowdDetectorOrionPublisher {

	public static final String LEVEL_ATTR = "Level";
	public static final String PERCENTAGE_ATTR = "Percentage";
	public static final String DIRECTION_ATTR = "Direction";

	@Autowired
	private OrionConnector orionConnector;

	/**
	 * Publi
	 *
	 * @param event
	 */
	public void publishEvent(CrowdDetectorDirectionEvent event) {
		OrionContextElement contextElement = directionEventToContextElement(event);
		this.orionConnector.updateContextElements(contextElement);
	}

	/**
	 * Publi
	 *
	 * @param event
	 */
	public void publishEvent(CrowdDetectorFluidityEvent event) {
		OrionContextElement contextElement = fluidityEventToContextElement(event);
		this.orionConnector.updateContextElements(contextElement);
	}

	/**
	 * Publi
	 *
	 * @param event
	 */
	public void publishEvent(CrowdDetectorOccupancyEvent event) {
		OrionContextElement contextElement = occupancyEventToContextElement(event);
		this.orionConnector.updateContextElements(contextElement);
	}

	public void registerRoisInOrion(Collection<RegionOfInterest> rois) {

		List<OrionContextElement> events = newArrayListWithCapacity(rois.size());

		for (RegionOfInterest roi : rois) {
			events.add(occupancyEventToContextElement(roi.getId()));
			events.add(directionEventToContextElement(roi.getId()));
			events.add(fluidityEventToContextElement(roi.getId()));
		}

		this.orionConnector.registerContextElements(events
				.toArray(new OrionContextElement[events.size()]));
	}

	private static OrionContextElement occupancyEventToContextElement(
			String roiID) {
		OrionContextElement contextElement = new OrionContextElement();

		contextElement.setId(roiID);
		contextElement.setPattern(false);
		contextElement.setType(CrowdDetectorOccupancyEvent.class
				.getSimpleName());

		OrionAttribute<Integer> attrLevel = new OrionAttribute<>(LEVEL_ATTR,
				Integer.class.getSimpleName(), Integer.valueOf(0));
		OrionAttribute<Float> attrPercentage = new OrionAttribute<>(
				PERCENTAGE_ATTR, Float.class.getSimpleName(), 0F);
		contextElement.getAttributes().add(attrLevel);
		contextElement.getAttributes().add(attrPercentage);

		return contextElement;
	}

	private static OrionContextElement occupancyEventToContextElement(
			CrowdDetectorOccupancyEvent event) {
		OrionContextElement contextElement = new OrionContextElement();

		contextElement.setId(event.getRoiID());
		contextElement.setType(event.getClass().getSimpleName());

		OrionAttribute<Integer> attrLevel = new OrionAttribute<>(LEVEL_ATTR,
				Integer.class.getSimpleName(), event.getOccupancyLevel());
		OrionAttribute<Float> attrPercentage = new OrionAttribute<>(
				PERCENTAGE_ATTR, Float.class.getSimpleName(),
				event.getOccupancyPercentage());
		contextElement.getAttributes().add(attrLevel);
		contextElement.getAttributes().add(attrPercentage);

		return contextElement;
	}

	private static OrionContextElement fluidityEventToContextElement(
			String roiID) {
		OrionContextElement contextElement = new OrionContextElement();

		contextElement.setId(roiID);
		contextElement.setPattern(false);
		contextElement
				.setType(CrowdDetectorFluidityEvent.class.getSimpleName());

		OrionAttribute<Integer> attrLevel = new OrionAttribute<>(LEVEL_ATTR,
				Integer.class.getSimpleName(), Integer.valueOf(0));
		OrionAttribute<Float> attrPercentage = new OrionAttribute<>(
				PERCENTAGE_ATTR, Float.class.getSimpleName(), 0F);
		contextElement.getAttributes().add(attrLevel);
		contextElement.getAttributes().add(attrPercentage);

		return contextElement;
	}

	private static OrionContextElement fluidityEventToContextElement(
			CrowdDetectorFluidityEvent event) {
		OrionContextElement contextElement = new OrionContextElement();

		contextElement.setId(event.getRoiID());
		contextElement.setType(event.getClass().getSimpleName());

		OrionAttribute<Integer> attrLevel = new OrionAttribute<>(LEVEL_ATTR,
				Integer.class.getSimpleName(), event.getFluidityLevel());
		OrionAttribute<Float> attrPercentage = new OrionAttribute<>(
				PERCENTAGE_ATTR, Float.class.getSimpleName(),
				event.getFluidityPercentage());
		contextElement.getAttributes().add(attrLevel);
		contextElement.getAttributes().add(attrPercentage);

		return contextElement;
	}

	private static OrionContextElement directionEventToContextElement(
			String roiID) {
		OrionContextElement contextElement = new OrionContextElement();

		contextElement.setId(roiID);
		contextElement.setPattern(false);
		contextElement.setType(CrowdDetectorDirectionEvent.class
				.getSimpleName());

		OrionAttribute<Float> directionAttribute = new OrionAttribute<>(
				DIRECTION_ATTR, Float.class.getSimpleName(), Float.valueOf(0));

		contextElement.getAttributes().add(directionAttribute);
		return contextElement;
	}

	private static OrionContextElement directionEventToContextElement(
			CrowdDetectorDirectionEvent event) {
		OrionContextElement contextElement = new OrionContextElement();

		contextElement.setId(event.getRoiID());
		contextElement.setType(event.getClass().getSimpleName());

		OrionAttribute<Float> directionAttribute = new OrionAttribute<>(
				DIRECTION_ATTR, Float.class.getSimpleName(),
				event.getDirectionAngle());

		contextElement.getAttributes().add(directionAttribute);
		return contextElement;
	}

}
