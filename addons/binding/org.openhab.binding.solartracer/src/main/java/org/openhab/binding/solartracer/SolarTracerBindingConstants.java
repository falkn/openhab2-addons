/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.solartracer;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link SolarTracerBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Christian Falk - Initial contribution
 */
public class SolarTracerBindingConstants {

    private static final String BINDING_ID = "solartracer";

    // Config constants
    public static final String CONFIG_PORT = "port";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SOLAR_TRACER = new ThingTypeUID(
            BINDING_ID, "solartracer");

    // List of all Channel ids
    public static final String CHANNEL_BATT_VOLTAGE = "batt_voltage";
    public static final String CHANNEL_CHARGE_CURRENT = "charge_current";

    // MT-5 protocol constants

}
