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

var SORT_BY_KEY = "sortBy";
var SORT_ORDER_KEY = "sortOrder";
var SORT_BY_UPDATED = "Last Updated";
var SORT_BY_PROXIMITY = "Proximity";
var SORT_ORDER_ASC = "Ascending";
var SORT_ORDER_DESC = "Descending";

// ===========================================================
var Layout = $n2.Class({

    config: null,

    atlasDesign: null,

    dispatchService: null,

    sidePanelName: null,

    displayControl: null,

    currentLatitude: null,

    currentLongitude: null,

    currentLocationAvailable: null,

    initialize: function(opts_){
        var opts = $n2.extend({
            config: null
        },opts_);

        var _this = this;

        this.config = opts.config;
        if( typeof this.config !== 'object' ){
            throw 'n2.cordovaLayout.Layout.initialize() config must be specified';
        }
        this.atlasDesign = this.config.atlasDesign;
		if( this.config.directory ){
			this.dispatchService = this.config.directory.dispatchService;
		}

		// dispatcher
		var d = this.dispatchService;
		if( d ){
		    var f = function(m, addr, dispatcher){
		        _this._handle(m, addr, dispatcher);
		    };
			d.register(DH,'unselected',f);
			d.register(DH, 'sortInitiate', f);
			d.register(DH, 'refreshListView', f);
		}

        // Listen to the Create Document callback from the native app
        document.addEventListener("deviceready", function () {
            window.nunaliit2.cordovaPlugin.registerCallback('onCreateDocument',
                function () {
                    window.onCreateDocument = function () {
                        d.send(DH, {
                            type: 'editInitiate'
                            , doc: {}
                        });
                    };
                }, function (error) {
                    console.error('Error on cordova callback invocation: ', error);
                });
        });

        document.addEventListener("deviceready", function () {
            window.nunaliit2.cordovaPlugin.registerCallback('onSortDocuments',
                function () {
                    // cordova.searchbar.show();
                    window.onSortDocuments = function () {
                        d.send(DH, {
                            type: 'sortInitiate'
                        });
                    };
                }, function (error) {
                    console.error('Error on cordova callback invocation: ', error);
                });

        });

        // Listen to the Search Documents callback from the native app
        document.addEventListener("deviceready", function () {
            window.nunaliit2.cordovaPlugin.registerCallback('onSearchDocuments',
                function () {
                    window.onSearchDocuments = function () {
                        //TODO: show search box
                        d.send(DH, {
                            type: 'searchInitiate',
                            searchLine: 'island'
                        });
                    };
                }, function (error) {
                    console.error('Error on cordova callback invocation: ', error);
                });
        });

        document.addEventListener("deviceready", evt => {
                navigator.geolocation.getCurrentPosition(
                    position => {
                        $n2.log("Current position callback rx'd: " + position.coords.longitude + ", " + position.coords.latitude);
                        _this.currentLatitude = position.coords.latitude;
                        _this.currentLongitude = position.coords.longitude;
                        _this.currentLocationAvailable = true;
                        // Need to refresh list to trigger sorting since the callback could be called after we've displayed the list.
                        d.send(DH, {
                            type: 'refreshListView'
                        });
                    },
                    positionError => {
                        $n2.log("Current position callback error: code: " + positionError.code + " msg: " + positionError.message);
                    })
            },
            false);

        this._display();
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
        }

        // Editor
        if( config.couchEditor ){
		    config.couchEditor.setPanelName(this.sidePanelName);
		    config.couchEditor.setSchemas( $n2.couchEdit.Constants.ALL_SCHEMAS );
        }

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
        }

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
        }
    },

    _registerDisplayControl: function(displayControl){
        this.displayControl = displayControl;

        this._displayAllDocuments();
    },

    _displayAllDocuments: function(){
        var _this = this;

        // For testing, can't seem to get a location from the emulator...
        // if (!this.currentLocationAvailable) {
        //     this.currentLatitude = 45.426;
        //     this.currentLongitude = -75.687;
        //     this.currentLocationAvailable = true;
        //     $n2.log("TEST: using pretend location: " + this.currentLongitude + ", " + this.currentLatitude);
        // }

        if( this.atlasDesign ){
            this.atlasDesign.queryView({
                viewName: 'info'
                ,onSuccess: function(rows){

                    // $n2.log('TRACE: before sort: ' + JSON.stringify(rows));

                    _this._sortDocuments(rows);

                    // $n2.log('TRACE: after sort: ' + JSON.stringify(rows));

                    var docIds = [];
                    for(var i=0,e=rows.length; i<e; ++i){
                        var row = rows[i];
                        var docId = row.value.id; // just cute to test value
                        docIds.push(docId);
                    }
                    $n2.log('Atlas contains '+docIds.length+' document(s)');

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
        }
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
		}
	},

	_sendSynchronousMessage: function(m){
		var d = this.dispatchService;
		if( d ){
			d.synchronousCall(DH,m);
		}
	},

	_handle: function(m, addr, dispatcher){
	    if( 'unselected' === m.type ){
		    this._displayAllDocuments();
	    } else if (m.type === 'sortInitiate') {
	        this._showSortDialog();
        } else if (m.type === 'refreshListView') {
	        this._displayAllDocuments();
        }
	},

    _sortDocuments: function(rows) {
        var localStorage = $n2.storage.getLocalStorage();
        var sortBy = localStorage.getItem(SORT_BY_KEY);
        var sortOrder = localStorage.getItem(SORT_ORDER_KEY);
        $n2.log("sortBy: " + sortBy + ", sortOrder: " + sortOrder);

        var ascending = true;
        if (sortOrder === SORT_ORDER_DESC) {
            ascending = false;
        }

        // Default sort by last updated time.
        if (!sortBy || sortBy === SORT_BY_UPDATED) {
            this._sortByUpdatedTime(rows, ascending);
        } else if (sortBy === SORT_BY_PROXIMITY) {
            this._sortByProximity(rows, ascending);
        }
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

    _sortByProximity: function (rows, ascending) {
        if (this.currentLocationAvailable) {
            $n2.log("Sort by proximity, current location: " + this.currentLongitude + ", " + this.currentLatitude);
            var _this = this;
            if (ascending) {
                rows.sort(function (a, b) {
                    //$n2.log("a.coords: " + a.value.lon + ", " + a.value.lat + " b.coords: " + b.value.lon + ", " + b.value.lat);
                    var distanceA = _this._getDistanceFromLatLonInKm(a.value.lat, a.value.lon, _this.currentLatitude, _this.currentLongitude);
                    var distanceB = _this._getDistanceFromLatLonInKm(b.value.lat, b.value.lon, _this.currentLatitude, _this.currentLongitude);

                    return distanceA - distanceB;
                });
            } else {
                rows.sort(function (a, b) {
                    //$n2.log("a.coords: " + a.value.lon + ", " + a.value.lat + " b.coords: " + b.value.lon + ", " + b.value.lat);
                    var distanceA = _this._getDistanceFromLatLonInKm(a.value.lat, a.value.lon, _this.currentLatitude, _this.currentLongitude);
                    var distanceB = _this._getDistanceFromLatLonInKm(b.value.lat, b.value.lon, _this.currentLatitude, _this.currentLongitude);

                    return distanceB - distanceA;
                });
            }
        } else {
            $n2.log("Current location unavailable, can't sort by proximity")
        }
    },

    /**
     *
     * @param lat1 First coordinate latitude
     * @param lon1 First coordinate longitude
     * @param lat2 Second coordinate latitude
     * @param lon2 Second coordinate longitude
     * @returns The distance between coords 1 and coords 2, in kilometres.
     * @private
     */
    _getDistanceFromLatLonInKm: function (lat1, lon1, lat2, lon2) {
        if ((!lat1 || !lon1) && (lat2 && lon2)) {
            // coords 1 are undefined
            return Number.MAX_SAFE_INTEGER;
        } else if ((lat1 && lon1) && (!lat2 || !lon2)) {
            // coords 2 are undefined
            return Number.MAX_SAFE_INTEGER;
        } else if ((!lat1 || !lon1) && (!lat2 || !lon2)) {
            // Both sets of coords are undefined, so they are equal
            return 0;
        }

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
    },

    _showSortDialog: function () {
        var _this = this;
        var sortBy = localStorage.getItem(SORT_BY_KEY);
        var sortOrder = localStorage.getItem(SORT_ORDER_KEY);

        var dialogId = $n2.getUniqueId();
        var sortBySelectId = $n2.getUniqueId();
        var sortOrderSelectId = $n2.getUniqueId();
        var $dialog = $('<div id="' + dialogId + '">'
            + '<div class="sortOptionsDialogPadding">'
            + '<label for="' + sortBySelectId + '">' + _loc('Sort by:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;') + '</label>'
            + '<select id="' + sortBySelectId + '"></select>'
            + '</div>'
            + '<br/>'
            + '<div class="sortOptionsDialogPadding">'
            + '<label for="' + sortOrderSelectId + '">' + _loc('Sort order:&nbsp;&nbsp;') + '</label>'
            + '<select id="' + sortOrderSelectId + '"></select>'
            + '</div>'
            + '<br/>'
            + '<div class="sortOptionsDialogButtonDiv"><button>' + _loc('OK') + '</button>&nbsp;&nbsp;<button>' + _loc('Cancel') + '</button></div>'
            + '</div>');
        // Add to 'Sort by' options
        var $sortBySelect = $dialog.find('#' + sortBySelectId);
        // $sortBySelect.append($('<option' + (sortBy === SORT_BY_DESCRIPTION ? ' selected' : '') + '>' + SORT_BY_DESCRIPTION + '</option>'));
        $sortBySelect.append($('<option' + (sortBy === SORT_BY_UPDATED ? ' selected' : '') + '>' + SORT_BY_UPDATED + '</option>'));
        $sortBySelect.append($('<option' + (sortBy === SORT_BY_PROXIMITY ? ' selected' : '') + '>' + SORT_BY_PROXIMITY + '</option>'));

        var $sortOrderSelect = $dialog.find('#' + sortOrderSelectId);
        $sortOrderSelect.append($('<option' + (sortOrder === SORT_ORDER_ASC ? ' selected' : '') + '>' + SORT_ORDER_ASC + '</option>'));
        $sortOrderSelect.append($('<option' + (sortOrder === SORT_ORDER_DESC ? ' selected' : '') + '>' + SORT_ORDER_DESC + '</option>'));

        $dialog.find('button')
            .first() // OK button
            .button({icons: {primary: 'ui-icon-check'}})
            .click(function () {
                var $dialog = $('#' + dialogId);
                var $sortBySelect = $dialog.find('#' + sortBySelectId);
                var $sortOrderSelect = $dialog.find('#' + sortOrderSelectId);
                var sortBy = $sortBySelect.val();
                var sortOrder = $sortOrderSelect.val();

                var localStorage = $n2.storage.getLocalStorage();
                localStorage.setItem('sortBy', sortBy);
                localStorage.setItem('sortOrder', sortOrder);

                $dialog.dialog('close');

                _this.dispatchService.send(DH, {
                    type: 'refreshListView'
                });

                return false;
            })
            .next() // Cancel button
            .button({icons: {primary: 'ui-icon-cancel'}})
            .click(function () {
                var $dialog = $('#' + dialogId);
                $dialog.dialog('close');
                return false;
            });

        var dialogOptions = {
            autoOpen: true,
            title: _loc('Sort Documents'),
            modal: true,
            close: function (event, ui) {
                var diag = $(event.target);
                diag.dialog('destroy');
                diag.remove();
            }
        };
        $dialog.dialog(dialogOptions);
    }
});

// ===========================================================
$n2.cordovaLayout = {
	Layout: Layout
};

})(jQuery,nunaliit2);
