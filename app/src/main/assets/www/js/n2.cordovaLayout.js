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
    },

    _display: function(){
        var _this = this;

        // Quick access
        var config = this.config;
        var atlasDb = config.atlasDb;
        var atlasDesign = config.atlasDesign;
        var documentSource = config.documentSource;

        var $main = $('body');

        $('<div>')
            .addClass('nunaliit_header')
            .appendTo($main);
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

        if( this.atlasDesign ){
            this.atlasDesign.queryView({
                viewName: 'info'
                ,onSuccess: function(rows){
                    var docIds = [];
                    for(var i=0,e=rows.length; i<e; ++i){
                        var row = rows[i];
                        var docId = row.value.id; // just cute to test value
                        docIds.push(docId);
                    };
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
	}
});

// ===========================================================
$n2.cordovaLayout = {
	Layout: Layout
};

})(jQuery,nunaliit2);
