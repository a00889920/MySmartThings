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
		//capability "Switch"
		capability "Temperature Measurement"
        
        attribute	"state", "string"
        attribute	"extruder1TargetTemp", "string"
        attribute	"extruder1ActualTemp", "string"
        attribute	"bedTargetTemp", "string"
        attribute	"bedActualTemp", "string"
	}
}


	simulator {
		// TODO: define status and reply messages here
	}

    tiles(scale: 2) {
        //multiAttributeTile(name:"octothing", type:"generic", width:6, height:4) {
        //    tileAttribute("device.state", key: "PRIMARY_CONTROL") {
        //      attributeState "Offline", label: '${name}', icon:"st.Office.office19", backgroundColor:"#d3d3d3"
        //      attributeState "Refreshing", label: '${name}', icon:"st.Office.office19", backgroundColor:"#79b821"
        //      attributeState "Operational", label:'${name}', icon:"st.Office.office19", backgroundColor:"#79b821"
        //    }
        //}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
        valueTile("extruderTemp", "device.extruder1ActualTemp", width: 2, height: 2) {
            state("extruderTemp", label:'Extruder: ${currentValue}°',
                backgroundColors:[
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
         }
         valueTile("bedTemp", "device.bedActualTemp", width: 2, height: 2) {
            state("bedTemp", label:'Bed: ${currentValue}°',
                backgroundColors:[
                	[value: 0, color: "#d3d3d3"],
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
        }
      //main "octothing"
      //details (["octothing", "extruderTemp", "refresh"])
      main "extruderTemp"
      details (["extruderTemp", "bedTemp", "refresh"])
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    //log.debug descMap
    def body = new String(descMap["body"].decodeBase64())
    //log.debug "body: ${body}"
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
        if (result.containsKey("temperature")) {
        	log.debug "temps: ${result.temperature}"
            sendEvent(name: "extruder1ActualTemp", value: "${result.temperature.tool0.actual}")
            if (result.temperature.bed) {
            	sendEvent(name: "bedActualTemp", value: "${result.temperature.bed.actual}")
                } else {
                sendEvent(name: "bedActualTemp", value: 0)
                }
            }
    }
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    sendEvent(name: "state", value: "Refreshing")
    getDeviceInfo()
}

def refresh() {
	log.debug "Executing 'refresh'"
    sendEvent(name: "state", value: "Refreshing")
    getDeviceInfo()
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
