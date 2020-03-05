/*
Copyright (c) 2016, Geomatics and Cartographic Research Centre, Carleton
University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 - Neither the name of the Geomatics and Cartographic Research Centre,
   Carleton University nor the names of its contributors may be used to
   endorse or promote products derived from this software without specific
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/
;(function($,$n2){
"use strict";

var _loc = function(str,args){ return $n2.loc(str,'nunaliit2-cordova',args); };
var DH = 'n2.cordovaLayout';

// ===========================================================
var Layout = $n2.Class({

    config: null,

    atlasDesign: null,

    dispatchService: null,

    sidePanelName: null,

    displayControl: null,

    initialize: function(opts_){
        var opts = $n2.extend({
            config: null
        },opts_);

        var _this = this;

        this.config = opts.config;
        if( typeof this.config !== 'object' ){
            throw 'n2.cordovaLayout.Layout.initialize() config must be specified';
        };
        this.atlasDesign = this.config.atlasDesign;
		if( this.config.directory ){
			this.dispatchService = this.config.directory.dispatchService;
		};

		// dispatcher
		var d = this.dispatchService;
		if( d ){
		    var f = function(m, addr, dispatcher){
		        _this._handle(m, addr, dispatcher);
		    };
			d.register(DH,'unselected',f);
		};

        this._display();

        // Listen to the Create Document callback from the native app
        window.document.addEventListener("deviceready", function() {
            window.nunaliit2.cordovaPlugin.registerCallback('onCreateDocument',
                function() {
                    window.onCreateDocument = function() {
                        d.send(DH, {
                            type: 'editInitiate'
                            ,doc: {}
                        });
                    };
                }, function(error) {
                    console.error('Error on cordova callback invocation: ', error);
                });
        });

        // window.document.addEventListener("deviceready", function() {
        //     window.nunaliit2.cordovaPlugin.registerCallback('onSearchDocuments',
        //         function() {
        //             cordova.searchbar.show();
        //         // window.onSearchDocuments = function() {
        //         //         d.send(DH, {
        //         //             type: 'searchInitiate',
        //         //             searchLine: 'island'
        //         //         });
        //         //     };
        //         }, function(error) {
        //             console.error('Error on cordova callback invocation: ', error);
        //         });
        // });

        // Listen to the Search Documents callback from the native app
        // window.document.addEventListener("deviceready", function() {
        //     window.nunaliit2.cordovaPlugin.registerCallback('onSearchDocuments',
        //         function() {
        //             window.onSearchDocuments = function() {
        //                 d.send(DH, {
        //                     type: 'searchInitiate',
        //                     searchLine: 'island'
        //                 });
        //             };
        //         }, function(error) {
        //             console.error('Error on cordova callback invocation: ', error);
        //         });
        // });
    },

    _display: function(){
        var _this = this;

        // Quick access
        var config = this.config;
        var atlasDb = config.atlasDb;
        var atlasDesign = config.atlasDesign;
        var documentSource = config.documentSource;

        var $main = $('body');
        $main.addClass('n2_cordova');

		// Let the content start at the top since there is no header on Cordova
        $("<style type='text/css'> .nunaliit_content { top: 0 } </style>").appendTo("head");

        var $contentDiv = $('<div>')
            .addClass('nunaliit_content')
            .addClass('n2_content_contains_no_map')
            .addClass('n2_content_contains_no_search')
            .addClass('n2_content_contains_text')
            .appendTo($main);

		this.sidePanelName = $n2.getUniqueId();
        var $text = $('<div>')
            .attr('id',this.sidePanelName)
            .addClass('n2_content_text')
            .appendTo($contentDiv);

        // Side panel
        this._displayWelcomeMessage();

        // Install widgets
        {
            new $n2.widgetBasic.CreateDocumentWidget({
                containerId: $n2.getUniqueId()
                ,dispatchService: config.directory.dispatchService
                ,authService: config.directory.authService
                ,showAsLink: true
            });
        };

        // Editor
        if( config.couchEditor ){
		    config.couchEditor.setPanelName(this.sidePanelName);
		    config.couchEditor.setSchemas( $n2.couchEdit.Constants.ALL_SCHEMAS );
        };

        // Display
        var displayFormat = 'classic';

        var displayHandlerAvailable = false;
        var msg = {
            type: 'displayIsTypeAvailable'
            ,displayType: displayFormat
            ,isAvailable: false
            ,displayOptions: null
        };
        this._sendSynchronousMessage(msg);
        if( msg.isAvailable ){
            displayHandlerAvailable = true;
        };

        if( displayHandlerAvailable ){
            this._sendDispatchMessage({
                type: 'displayRender'
                ,displayType: displayFormat
                ,displayOptions: null
                ,displayId: this.sidePanelName
                ,config: this.config
                ,moduleDisplay: null
                ,onSuccess: function(displayControl){
                    _this._registerDisplayControl(displayControl);
                }
                ,onError: function(err){
                    alert('Unable to start display: '+err);
                }
            });
        } else {
            drawCanvas(searchInfo, mapInfo, canvasInfo);
        };
    },

    _registerDisplayControl: function(displayControl){
        this.displayControl = displayControl;

        this._displayAllDocuments();
    },

    _displayAllDocuments: function(){
        var _this = this;

        $n2.cordovaPlugin.echo({
            msg: 'client msg',
            onSuccess: function (msg) {
                if ('client msg (server)' === msg) {
                    $n2.log('SARAH: echo success: ' + msg);
                } else {
                    $n2.log('SARAH: echo error: Unexpected message (' + msg + ')');
                }
            },
            onError: function (err) {
                $n2.log('SARAH: echo error: ' + err);
            }
        });

        var currentLatitude;
        var currentLongitude;
        $n2.cordovaPlugin.getCurrentLocation({
            onSuccess: function(result) {
                if (result.hasOwnProperty("lon") && result.hasOwnProperty("lat")) {
                    currentLatitude = result.lat;
                    currentLongitude = result.lon;
                }

                if (currentLongitude && currentLatitude) {
                    $n2.log("SARAH: current location: " + currentLongitude + ", " + currentLatitude);
                }
                else {
                    $n2.log("SARAH: current location unavailable");
                }
            },
            onError: function(err) {
                $n2.log('SARAH: Error getting current location: ' + err);
            }
        })

        if( this.atlasDesign ){
            this.atlasDesign.queryView({
                viewName: 'info'
                ,onSuccess: function(rows){

                    $n2.log('SARAH: before sort: ' + JSON.stringify(rows));
                    _this._sortByUpdatedTime(rows, false);

                    $n2.log('SARAH: after sort: ' + JSON.stringify(rows));

                    var docIds = [];
                    for(var i=0,e=rows.length; i<e; ++i){
                        var row = rows[i];
                        var docId = row.value.id; // just cute to test value
                        docIds.push(docId);
                    };
                    $n2.log('Atlas contains '+docIds.length+' document(s)');

                    //SARAH: sort them

                    _this._sendDispatchMessage({
                        type: 'selected'
                        ,docIds: docIds
                    });
                }
                ,onError: function(err){
                    $n2.log('Unable to obtain list of all documents');
                }
            });
        } else {
            this._displayWelcomeMessage();
        };
    },

    _displayWelcomeMessage: function() {
        var _this = this;

        var $elem = $('#'+this.sidePanelName);

        $elem.text('intro');
    },

	_sendDispatchMessage: function(m){
		var d = this.dispatchService;
		if( d ){
			d.send(DH,m);
		};
	},

	_sendSynchronousMessage: function(m){
		var d = this.dispatchService;
		if( d ){
			d.synchronousCall(DH,m);
		};
	},

	_handle: function(m, addr, dispatcher){
	    if( 'unselected' === m.type ){
		    this._displayAllDocuments();
	    };
	},

	_sortByUpdatedTime: function(rows, ascending) {
        if (ascending) {
            rows.sort(function(a, b) {
                return parseInt(a.value.updatedTime) - parseInt(b.value.updatedTime);
            });
        } else {
            rows.sort(function(a, b) {
                return parseInt(b.value.updatedTime) - parseInt(a.value.updatedTime);
            });
        }
    },

    _sortByLocation: function(rows, ascending) {
        if (ascending) {
            rows.sort(function(a, b) {
                return parseInt(a.value.updatedTime) - parseInt(b.value.updatedTime);
            });
        } else {
            rows.sort(function(a, b) {
                return parseInt(b.value.updatedTime) - parseInt(a.value.updatedTime);
            });
        }
    },

    _sortByDistanceToCurrentLocation: function(rows, ascending) {
        if (ascending) {
            rows.sort(function(a, b) {

            });
        } else {
            rows.sort(function(a, b) {

            });
        }
    },

    _getDistanceFromLatLonInKm: function (lat1, lon1, lat2, lon2) {
        var R = 6371; // Radius of the earth in km
        var dLat = this._deg2rad(lat2 - lat1);  // deg2rad below
        var dLon = this._deg2rad(lon2 - lon1);
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(this._deg2rad(lat1)) * Math.cos(this._deg2rad(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);

        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    },

    _deg2rad: function (deg) {
        return deg * (Math.PI / 180)
    }
});

// ===========================================================
$n2.cordovaLayout = {
	Layout: Layout
};

})(jQuery,nunaliit2);
