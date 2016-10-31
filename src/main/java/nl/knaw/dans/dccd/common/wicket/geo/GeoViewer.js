/*******************************************************************************
 * Copyright 2015 DANS - Data Archiving and Networked Services
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
// GeoViewer 

// the one and only global var serves as a sort of 'namespace'
var geoViewer = {
	// Works with only one map/viewer per page, 
	// but should have a list with JS objects for each view and have a lookup by the markup id
	Instance : null,
	// Initialize the (single) viewer
	init : function (id, options) {
		this.Instance = new GeoViewer(id, options);
	}, 
	
	// Add a marker at the given location on the map
	addMarker : function (lon, lat, info) {
		this.Instance.addMarker(lon, lat, info);
	}, 

	addMarkers : function (markerList) {
		this.Instance.addMarkers(markerList);
	}, 

	// Remove all markers from the map
	clearMarkers : function () {
		this.Instance.clearMarkers();
	}, 

	clustering : false,
	
	// Marker Icons can be shared among viewers
	DefaultMarkerIconPath : "", // 'global setting' must be set prior to creating viewers!
	DefaultMarkerIcon : null,
	LetterIcons : [],
	hasInitializedMarkerIcons: false,
	initMarkerIcons : function () {
		// Load all icon images for the markers, same for all viewers
		// Only run once, the first GeoViewer() call will do this! 
		if (this.hasInitializedMarkerIcons) return;
		
		this.hasInitializedMarkerIcons = true;
		
		// initialize marker icon(s)
		var iconSize = new OpenLayers.Size(16, 25);
		var iconOffset = new OpenLayers.Pixel(-(iconSize.w/2), -iconSize.h);
		this.DefaultMarkerIcon = new OpenLayers.Icon(this.DefaultMarkerIconPath, iconSize, iconOffset);
		
		// load all Letter icons using the defaulticon name as a basis
		// remove extension ".png"
		var markerIconFileBasename = geoViewer.DefaultMarkerIconPath.substr(0, this.DefaultMarkerIconPath.length -4);
		var markerIconFileExtension = geoViewer.DefaultMarkerIconPath.substr(this.DefaultMarkerIconPath.length -3, 3);	
		
		for (var i=0; i<26; i++) {
			var c = String.fromCharCode(65+i);
			var markerIconFilename = markerIconFileBasename + "_" + c + "." + markerIconFileExtension;
			//this.letterIcons[i] = new OpenLayers.Icon(markerIconFilename, this.iconSize, this.iconOffset);
			this.LetterIcons[i] = new OpenLayers.Icon(markerIconFilename, iconSize, iconOffset);
		}
	}
};

// The viewer object/class (constructor like function)
//
// Note:
// The maps are in (google's) spherical mercator (EPSG:900913)
// But the Marker locations are in WGS84 (EPSG:4326)
function GeoViewer(id, options) {
	//this.clustering = geoViewer.clustering;
	this.options = options || {
		// default options here
		clustering: geoViewer.clustering
	};
	// use the options
	this.clustering = this.options.clustering;
	
	geoViewer.initMarkerIcons();

	// avoid pink tiles
	OpenLayers.IMAGE_RELOAD_ATTEMPTS = 3;
	OpenLayers.Util.onImageLoadErrorColor = "transparent";
	
	// Initialize the (map) viewer
    this.map = new OpenLayers.Map({
        div: id,
        projection: new OpenLayers.Projection("EPSG:900913"),
        units: "m",
//        maxResolution: 156543.0339,
//        maxExtent: new OpenLayers.Bounds(-20037508, -20037508, 
//        		20037508, 20037508.34),
        controls: [
                   new OpenLayers.Control.Navigation(), // always need this for interactive maps
                   new OpenLayers.Control.PanZoom(), //new OpenLayers.Control.PanZoomBar()
                   new OpenLayers.Control.Attribution()
        ]
    });

	this.layers = [];
	// OpenLayers with OpenStreetMaps as base layer
	//var osm = new OpenLayers.Layer.OSM();
	// need to use specify https explicitly, to prefent 'mixed content' warning
	var osm = new OpenLayers.Layer.OSM("OpenStreetMap",
			["https://a.tile.openstreetmap.org/${z}/${x}/${y}.png",
			"https://b.tile.openstreetmap.org/${z}/${x}/${y}.png",
			"https://c.tile.openstreetmap.org/${z}/${x}/${y}.png"]);

//	var nat = new OpenLayers.Layer.XYZ(
//            "Natural Earth", 
//            ["https://a.tiles.mapbox.com/v3/mapbox.natural-earth-hypso-bathy/${z}/${x}/${y}.png"],
//            {attribution: "Tiles ������ MapBox", 
//            	sphericalmercator: true,
//            	wrapDateLine: true,
//            	numZoomLevels: 19
//            });
//	

	this.layers[0] = osm;
//	this.layers[1] = nat;
	this.map.addLayers(this.layers);

	// ------------------
	// provide for a separate marker layer
	function onMarkerPopupClose(evt){
		selector.unselectAll();
	};
	
	// if there is more than one marker in the cluster we list the info
	// but we restrict this to a maximum number of markers, otherwise the popup becomes unusable.
	// Also note that, when we have several markers on exactly the same possition, 
	// we would not have a popup if this max was 1 
	// and we would not be able to view the information of any of those markers. 
	var popupMaxClusterSize = 9; // must be 1 or bigger
	
 	this.onFeatureUnselect = function onFeatureUnselect(evt){
        var feature = evt.feature;
        if (!(feature.cluster===undefined)) {
        	// skip clusters without popup
        	if (feature.cluster.length > popupMaxClusterSize) return; 
        }
        
        this.map.removePopup(feature.popup);
        feature.popup.destroy();
        feature.popup = null;   	
 	};
 	
 	this.onFeatureSelect = function onFeatureSelect(evt){
 		var callBack = onMarkerPopupClose;
        var feature = evt.feature;
        if (feature.cluster===undefined){
        	// no clustering
            var popup = new OpenLayers.Popup.FramedCloud("popup",
                    OpenLayers.LonLat.fromString(feature.geometry.toShortString()),
                    new OpenLayers.Size(200, 200),
                    feature.attributes.info,
                    null, 
                    true, 
                    callBack // need to set our own callback here for the popup close box 
                );
                feature.popup = popup;
                this.map.addPopup(popup, true);
        } else {
        	// clustering
        	var contentHtml = "";

        	// only popup for cluster size 1        	
            if (feature.cluster.length > popupMaxClusterSize) {
            	return; 
            } else {
            	contentHtml = feature.cluster[0].attributes.info;
            	// now add the others if needed
            	for(var i = 1; i < feature.cluster.length; i++) {
            		contentHtml += "<hr/>" + feature.cluster[i].attributes.info;
            	}
            }
        	
        	var popup = new OpenLayers.Popup.FramedCloud("popup",
                    OpenLayers.LonLat.fromString(feature.geometry.toShortString()),
                    new OpenLayers.Size(200, 200),
                    contentHtml, 
                    null, 
                    true, 
                    callBack // need to set our own callback here for the popup close box 
                );
            feature.popup = popup;
            this.map.addPopup(popup, true);
        }

 	};
 	
 	//var clustering = true; // emulate a setting

 	var sm;
 	var strats = [];
 	if (this.clustering) { 
 	 	sm = createClusterStyleMap();
 	 	strats = [new OpenLayers.Strategy.Cluster({
 			distance: 45
 		})]; 		
 	} else {
 		sm = createLetterStyleMap();//createDefaultStyleMap(); 
 	}
 	this.markers = new OpenLayers.Layer.Vector('markers', {
        eventListeners:{
		'featureselected':this.onFeatureSelect,
		'featureunselected':this.onFeatureUnselect
        }, 
		// -------
		renderers: ['Canvas','SVG','VML'],
        strategies: strats,             
        styleMap: sm
	}); 	
/* 
 	var sm = createLetterStyleMap();//createDefaultStyleMap(); 
 	this.markers = new OpenLayers.Layer.Vector('markers', {
 			strategies: [],
 			renderers: ['Canvas','SVG','VML'], // needed for clustering, but does not harm
 			styleMap: sm, 
            eventListeners:{
    		'featureselected':this.onFeatureSelect,
    		'featureunselected':this.onFeatureUnselect
            }
 	});
 */	
 /*	
 	var sm = createClusterStyleMap();
 	var strat = new OpenLayers.Strategy.Cluster({
		distance: 45
	}); 	
 	this.markers = new OpenLayers.Layer.Vector('markers', {
            eventListeners:{
    		'featureselected':this.onFeatureSelect,
    		'featureunselected':this.onFeatureUnselect
            }, 
			// -------
			renderers: ['Canvas','SVG','VML'],
            strategies: [strat],             
            styleMap: sm
 	});
 	strat.activate();
 */ 	
 	
 	//this.markers = new OpenLayers.Layer.Markers("Markers"); 
 	this.map.addLayer(this.markers); 
 	selector = new OpenLayers.Control.SelectFeature(this.markers, {
 		clickout: true, // this is always nice especially without a close box
 		toggle: false,//false, // when we have no close box toggle is necessary
 		multiple: false, 
 		hover: false
 		//autoActivate:true
	}); 	
 	this.map.addControl(selector);
 	selector.activate();
	// ---------------------
 	
 	
	
	// with several layers you need a switch, also allows to toggle markers
	this.map.addControl(new OpenLayers.Control.LayerSwitcher());
	// show position, maybe make it optional?
	//this.map.addControl(new OpenLayers.Control.MousePosition());
	// OverviewMap might be nice as well for large map views
	
	// show europe
	var lonLat = new OpenLayers.LonLat(9.0, 54.0).
			transform(new OpenLayers.Projection("EPSG:4326"), new OpenLayers.Projection("EPSG:900913")); 
	this.map.setCenter (lonLat, 3);
	
	// Add a marker at the given location on the map
	this.addMarker = function(lon, lat, info) {
		var order = 0;
		if (this.markers.features.length != undefined) order = this.markers.features.length;

		var position = new OpenLayers.Geometry.Point(lon,lat).transform( 'EPSG:4326', 'EPSG:900913');
		var marker = new OpenLayers.Feature.Vector(position, {'info' : info, 'order': order});
		this.markers.addFeatures([marker]);	
	};
	
	// Remove all markers from the map
	this.clearMarkers = function () {
		// Destroy all markers first
		for (var i=0; i < this.markers.features.length; i++) {
			this.markers.features[i].destroy();
		}
		// then remove them from the markers array
		this.markers.removeAllFeatures();
		// TODO check if there is one call that does it all
		//this.markers.destroyFeatures();
	};
	
	// example
	// [{lon:52.0,lat:4.9,info:"A"},{lon:52.01,lat:4.91,info:"B"}]
	// could compress JSON even further and make each marker an array!
	this.addMarkers = function(markerList)
	{
		// NOTE on clustering we need to add all features in one go
		// NO DYMNAMIC ADDING WITH CUSTERING !		
		var order = 0;
		// anticipate that there are some features allready
		if (this.markers.features.length != undefined) order = this.markers.features.length;

		var features = [];
		for (var i=0; i < markerList.length; i++) {
			var m = markerList[i];
			var position = new OpenLayers.Geometry.Point(m.lon, m.lat).transform( 'EPSG:4326', 'EPSG:900913');
			var feature = new OpenLayers.Feature.Vector(position, {'info' :  m.info, 'order': order});
			features.push(feature);
			order = order +1;
		}
		this.markers.addFeatures(features);
	};
	
	function createClusterStyleMap(){
		
		
		
        // Define three colors that will be used to style the cluster features
        // depending on the number of features they contain.
        var colors = {
            low: "rgb(255, 196, 16)", 
            middle: "rgb(255, 196, 16)", 
            high: "rgb(255, 196, 16)"
        };
        
        var singleRule = new OpenLayers.Rule({
            filter: new OpenLayers.Filter.Comparison({
                type: OpenLayers.Filter.Comparison.LESS_THAN,
                property: "count",
                value: 2
            }),
            symbolizer: {
            	fillOpacity: 1.0, 
    	 		externalGraphic: geoViewer.DefaultMarkerIcon.url, 
    	 		graphicWidth: geoViewer.DefaultMarkerIcon.size.w, // 20, 
    	 		graphicHeight: geoViewer.DefaultMarkerIcon.size.h, // 24, 
    	 		graphicYOffset: geoViewer.DefaultMarkerIcon.offset.y // -24
            }
        });
        
       // Define three rules to style the cluster features.
        var lowRule = new OpenLayers.Rule({
            filter: new OpenLayers.Filter.Comparison({
                type: OpenLayers.Filter.Comparison.BETWEEN,
                property: "count",
                lowerBoundary: 2,
                upperBoundary: 9
            }),
            symbolizer: {
                fillColor: colors.low,
                fillOpacity: 0.9, 
                strokeColor: colors.low,
                strokeOpacity: 0.5,
                strokeWidth: 12,
                pointRadius: 10,
                label: "${count}",
                labelOutlineWidth: 0,//1,
                fontColor: "#ffffff",
                fontOpacity: 0.8,
                fontSize: "12px",
                fontWeight:"bold",
                fontFamily:"Helvetica, Arial, sans-serif"
            }
        });
        var middleRule = new OpenLayers.Rule({
            filter: new OpenLayers.Filter.Comparison({
                type: OpenLayers.Filter.Comparison.BETWEEN,
                property: "count",
                lowerBoundary: 10,
                upperBoundary: 99
            }),
            symbolizer: {
                fillColor: colors.middle,
                fillOpacity: 0.9, 
                strokeColor: colors.middle,
                strokeOpacity: 0.5,
                strokeWidth: 12,
                pointRadius: 15,
                label: "${count}",
                labelOutlineWidth: 0,//1,
                fontColor: "#ffffff",
                fontOpacity: 0.8,
                fontSize: "12px",
                fontWeight:"bold",
                fontFamily:"Helvetica, Arial, sans-serif"
            }
        });
        var highRule = new OpenLayers.Rule({
            filter: new OpenLayers.Filter.Comparison({
                type: OpenLayers.Filter.Comparison.GREATER_THAN,
                property: "count",
                value: 99
            }),
            symbolizer: {
                fillColor: colors.high,
                fillOpacity: 0.8, 
                strokeColor: colors.high,
                strokeOpacity: 0.5,
                strokeWidth: 12,
                pointRadius: 20,
                label: "${count}",
                labelOutlineWidth: 0,//1,
                fontColor: "#ffffff",
                fontOpacity: 0.8,
                fontSize: "12px",
                fontWeight:"bold",
                fontFamily:"Helvetica, Arial, sans-serif"
            }
        });
        
        // Create a Style that uses the three previous rules
        var style = new OpenLayers.Style(null, {
            rules: [singleRule, lowRule, middleRule, highRule]
        });            
	
        var sm = new OpenLayers.StyleMap(style);
        return sm;
	}
	
	// use balloon with a letter, but when more than 26 markers 
	// use default balloon for those
	function createLetterStyleMap(){
	 	var lowRule = new OpenLayers.Rule({
	        filter: new OpenLayers.Filter.Comparison({
	            type: OpenLayers.Filter.Comparison.LESS_THAN,
	            property: "order",
	            value: 26
	        }),
	        symbolizer: {
	        	fillOpacity: 1.0, 
	        	//externalGraphic: geoViewer.LetterIcons['${order}'],
	     		graphicWidth: geoViewer.DefaultMarkerIcon.size.w, // 20, 
	     		graphicHeight: geoViewer.DefaultMarkerIcon.size.h, // 24, 
	     		graphicYOffset: geoViewer.DefaultMarkerIcon.offset.y // -24
	        }
	    });
	    var highRule = new OpenLayers.Rule({
	        filter: new OpenLayers.Filter.Comparison({
	            type: OpenLayers.Filter.Comparison.GREATER_THAN,
	            property: "order",
	            value: 26
	        }),
	        symbolizer: {
	        	fillOpacity: 1.0, 
	        	externalGraphic: geoViewer.DefaultMarkerIcon.url,//'http://openlayers.org/dev/img/marker.png',
	     		graphicWidth: geoViewer.DefaultMarkerIcon.size.w, // 20, 
	     		graphicHeight: geoViewer.DefaultMarkerIcon.size.h, // 24, 
	     		graphicYOffset: geoViewer.DefaultMarkerIcon.offset.y // -24
	        }
	    });
	    var style = new OpenLayers.Style(null, {
	        rules: [lowRule, highRule]
	    });
	    var lookup = {};
	    for (var order=0; order<26; order++){
	    	lookup[order] = {'externalGraphic' : geoViewer.LetterIcons[order].url};
	    }
	    var sm = new OpenLayers.StyleMap(style);
	    sm.addUniqueValueRules("default", "order", lookup);
	    return sm;
	}
	
	// use default balloon for all markers
	function createDefaultStyleMap(){
	 	var sm = new OpenLayers.StyleMap({
	 		externalGraphic: geoViewer.DefaultMarkerIcon.url,//'http://openlayers.org/dev/img/marker.png',
	 		graphicWidth: geoViewer.DefaultMarkerIcon.size.w, // 20, 
	 		graphicHeight: geoViewer.DefaultMarkerIcon.size.h, // 24, 
	 		graphicYOffset: geoViewer.DefaultMarkerIcon.offset.y // -24
	 	});
	 	return sm;
	}
	
	// TEST
    function addRandomMarkers(markersLayer)
    {
            // Create some random features
            var features = [];
            for(var i=0; i< 5000; i++) {
                var lon = Math.random() * 2 + 4.9;
                var lat = Math.random() * 2 + 52.0;
                
                var lonlat = new OpenLayers.LonLat(lon, lat);
                lonlat.transform(new OpenLayers.Projection("EPSG:4326"), new OpenLayers.Projection("EPSG:900913"));

                var f = new OpenLayers.Feature.Vector( new OpenLayers.Geometry.Point(lonlat.lon, lonlat.lat), 
                {info:'some <i>html</i> <b>text</b>' + ' num: ' + i});
                features.push(f);
            }
            markersLayer.addFeatures(features); 
    }
}
