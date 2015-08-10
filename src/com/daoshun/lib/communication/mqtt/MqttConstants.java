/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.daoshun.lib.communication.mqtt;

/**
 * Static constants for this package.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class MqttConstants {

    public static final String SHARED_PREFERENCE_NAME = "mqtt_client_preferences";

    // PREFERENCE KEYS
    public static final String MQTT_HOST = "MQTT_HOST";

    public static final String MQTT_PORT = "MQTT_PORT";

    public static final String MQTT_STARTED = "MQTT_STARTED";

    public static final String MQTT_DEVICE_ID = "MQTT_DEVICE_ID";

    public static final String MQTT_RETRY = "MQTT_RETRY";

    public static final String MQTT_RECEIVED_ACTION = "MQTT_RECEIVED_ACTION";

    // NOTIFICATION FIELDS
    public static final String NOTIFICATION_TOPIC = "NOTIFICATION_TOPIC";

    public static final String NOTIFICATION_MESSAGE = "NOTIFICATION_MESSAGE";
}
