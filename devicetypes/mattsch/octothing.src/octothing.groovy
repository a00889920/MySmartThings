/**
 *  OctoThing
 *
 *  Copyright 2015 Matthew Schick
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.JsonSlurper

preferences {
    section("OctoThing Setup"){
        input("ip", "string", title:"IP Address", description: "Ip Address of OctoPrint", required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "Port (usually 80)", defaultValue: 80, required: true, displayDuringSetup: true)
        input("apiKey", "string", title:"API Key", description: "API Key", required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "OctoThing", namespace: "mattsch", author: "Matthew Schick") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Temperature Measurement"
        
        attribute	"state", "string"
        attribute	"extruder1TargetTemp", "number"
        attribute	"extruder1ActualTemp", "number"
        attribute	"bedTargetTemp", "number"
        attribute	"bedActualTemp", "number"
	}
}


	simulator {
		// TODO: define status and reply messages here
	}

    tiles(scale: 2) {
        multiAttributeTile(name:"octothing", type:"generic", width:6, height:4) {
            tileAttribute("device.state", key: "PRIMARY_CONTROL") {
              attributeState "Offline", label: '${name}', icon:"st.Office.office19", backgroundColor:"#d3d3d3"
              attributeState "Refreshing", label: '${name}', icon:"st.Office.office19", backgroundColor:"#79b821"
              attributeState "Operational", label:'${name}', icon:"st.Office.office19", backgroundColor:"#79b821"
            }
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
      main "octothing"
      details (["octothing", "refresh"])
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    //log.debug descMap
    def body = new String(descMap["body"].decodeBase64())
    log.debug "body: ${body}"
    def result
    try {
        def slurper = new JsonSlurper()
        result = slurper.parseText(body)
    } catch (ex) {
    	sendEvent(name: "state", value: "Offline")
    }
	if (result) {
        log.debug "result: ${result}"
        if (result.containsKey("state")) {
                log.debug "state: ${result.state.text}"
                sendEvent(name: "state", value: result.state.text)
            }
    }
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    sendEvent(name: "state", value: "Refreshing")
    getDeviceInfo()
	// TODO: handle 'poll' command
}

def refresh() {
	log.debug "Executing 'refresh'"
    sendEvent(name: "state", value: "Refreshing")
    getDeviceInfo()
	// TODO: handle 'refresh' command
}

def on() {
	log.debug "Executing 'on'"
	// TODO: handle 'on' command
}

def off() {
	log.debug "Executing 'off'"
	// TODO: handle 'off' command
}

private getDeviceInfo() {
def uri = "/api/printer?history=no&exclude=sd"
setDeviceNetworkId(ip,port)
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri,
    headers: [
    	HOST: getHostAddress(),
        "X-Api-Key": "${apiKey}",
       ],
  )//,delayAction(1000), refresh()]
  log.debug("Executing hubAction on " + getHostAddress())
  //log.debug hubAction
  hubAction
}
  
private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}
