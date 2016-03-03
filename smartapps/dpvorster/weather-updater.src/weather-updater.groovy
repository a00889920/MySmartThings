/**
* Manual Weather Refresh
*
* Copyright 2015 Daniel Vorster
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
*/
definition(
name: "Weather Updater",
namespace: "dpvorster",
author: "Daniel Vorster",
description: "Refreshes weather tile",
category: "Convenience",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

section {
input "devices", "capability.temperatureMeasurement", title: "When these devices sends events", multiple: true, required: true
input "weather", "device.smartweatherStationTile", title: "Update this weather device:", reguired: true
}

def installed() {
log.debug "Installed with settings: ${settings}"
initialize()
}

def updated() {
log.debug "Updated with settings: ${settings}"
unsubscribe()
initialize()
}

def initialize() {
subscribe(devices, "temperature", appHandler)
weather.refresh()
}

def appHandler(evt) {
weather.refresh()
}