/*
Copyright (c) 2016, Geomatics and Cartographic Research Centre, Carleton University
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

// Localization
var _loc = function(str,args){ return $n2.loc(str,'nunaliit2-cordova',args); };

var DH = 'n2.cordovaAuth';

// ===================================================================================

var defaultError = function(err, options) {
	var acc = [];

	if( err ) {
		acc.push(''+err.message);
		var cause = err.cause;
		while( cause ) {
			acc.push('\n>'+cause.message);
			cause = cause.cause;
		};
	} else {
		acc.push( _loc('<Unknown error>') );
	};

	alert(acc.join(''));
};

var AuthService = $n2.Class({
	options: null

	,connectionInfo: null

	,couchServer: null

	,loginStateListeners: null

	,lastAuthSessionCookie: null

	,lastSessionContext: null

	,userServiceAvailable: null

	,autoRegistrationAvailable: null

	,currentUserDoc: null

	,initialize: function(options_){
		var _this = this;

		this.options = $n2.extend(
			{
				onSuccess: function(result,options) {}
				,onError: defaultError
				,atlasDb: null
				,schemaRepository: null
				,disableCreateUserButton: false
				,directory: null
				,listeners: null
				,autoRefresh: true
				,prompt: _loc('Please login')
				,refreshIntervalInSec: 2 // 120 // 2 minutes
				,userServerUrl: null
			}
			,options_
		);

		var _this = this;

		this.loginStateListeners = [];
		this.lastAuthSessionCookie = null;
		this.lastSessionContext = null;
		this.userServiceAvailable = false;
		this.autoRegistrationAvailable = false;

		// Install login state listeners - don't retain as stored options.
		if( this.options.listeners ) {
			this.addListeners(this.options.listeners);
			delete this.options.listeners;
		};

		/*
		 * carry either default or provided fns for onSuccess or onError
		 * and remove these from the stored options ... they are usually
		 * not appropriate for use as login and logout callbacks.
		 */
		var initOnSuccess = this.options.onSuccess;
		delete this.options.onSuccess;
		var initOnError = this.options.onError;
		delete this.options.onError;

		var optWithCallbacks = $n2.extend({}, // use this as init callback
			this.options,
			{
				onSuccess: initOnSuccess
				,onError: initOnError
			}
		);

		// Listen to events
		var dispatcher = this._getDispatcher();
		if( dispatcher ){
			var fn = function(m){
				_this._handleEvent(m);
			};
			dispatcher.register(DH,'login',fn);
			dispatcher.register(DH,'loginShowForm',fn);
			dispatcher.register(DH,'logout',fn);
			dispatcher.register(DH,'authIsLoggedIn',fn);
		};

		$n2.cordovaPlugin.getConnectionInfo({
            onSuccess: function(info){
                _this.connectionInfo = info;
                onSuccess(info);
            }
            ,onError: onError
		});

		function onSuccess(context) {
			$n2.log("Login successful");
			initOnSuccess(context, optWithCallbacks);
			_this._notifyListeners();
		};

		function onError(error) {
			$n2.log('Login(adjustCookies) error: '+error);

			var err = {
				message: 'Problem initializing authentication library'
				,cause: {
					message: error
				}
			};
			initOnError(err, optWithCallbacks);
			_this._notifyListeners();
		};
	}

	,addListeners: function(listeners) {
		var _this = this;
		var cUser = this._getCurrentListenerInfo();

		if( typeof(listeners) == 'function' ) {
			addListener(listeners);

		} else if( $n2.isArray(listeners) ) {
			for(var loop=0; loop<listeners.length; ++loop) {
				var listener = listeners[loop];
				if( typeof(listener) === 'function' ) {
					addListener(listener);
				};
			};
		};

		function addListener(listener) {
			_this.loginStateListeners.push(listener);
			try {
				listener(cUser);
			} catch(e) {
				$n2.log('CouchAuthService: EXCEPTION caught in listener (add)',e);
			};
		};
	}

	,_notifyListeners: function() {
		var context = this._getAuthContext();

		var userName = null;
		if( context ) {
			userName = context.name;
		};

		var isAdmin = false;
		if( context
		 && context.roles
		 && this.doRolesContainAdmin(context.roles) ){
			isAdmin = true;
		};

		// Notify via DOM classes
		var $body = $('body');
		if( userName ){
			$body.removeClass('nunaliit_logged_out');
			$body.addClass('nunaliit_logged_in');
		} else {
			$body.removeClass('nunaliit_logged_in');
			$body.removeClass('nunaliit_user_advanced');
			$body.addClass('nunaliit_logged_out');
		};
		if( isAdmin ) {
			$body.addClass('nunaliit_user_administrator');
		} else {
			$body.removeClass('nunaliit_user_administrator');
		};

		// Notify via dispatcher
		if( userName ){
			this._dispatch({
				type: 'authLoggedIn'
				,user: context
			});
		} else {
			this._dispatch({
				type: 'authLoggedOut'
			});
		};

		var cUser = this._getCurrentListenerInfo();
		for(var loop=0; loop<this.loginStateListeners.length; ++loop) {
			var listener = this.loginStateListeners[loop];
			if( listener ) {
				try {
					listener(cUser);
				} catch(e) {
					$n2.log('CouchAuthService: EXCEPTION caught in listener (notify)',e);
				};
			};
		};
	}

	,_getCurrentListenerInfo: function(){
		var context = this._getAuthContext();

		var info = null;
		if( context && context.name ){
			info = {
				name: context.name
				,roles: context.roles
			};
		};

		return info;
	}

	,login: function(opts_) {
		var opts = $n2.extend({
			username: null
			,password: null
			,onSuccess: function(context){}
			,onError: function(errMsg){}
		},opts_);

		opts.onError('Mobile application is always logged in');
	}

	,showLoginForm: function(opts_) {
		var opts = $.extend({
			prompt: this.options.prompt
			,onSuccess: function(context){}
			,onError: $n2.reportErrorForced
		}, opts_);

		opts.onError('Mobile application is always logged in');
	}

	,logout: function(opts_) {
		var opts = $n2.extend({
			onSuccess: function(context){}
			,onError: function(err){}
		},opts_);

        opts.onError( _loc('Mobile application never logs out') );
	}

	,editUser: function(opts_){
		var opts = $n2.extend({
			userName: null // null means current user
			,onSuccess: function(context){}
			,onError: function(err){}
		},opts_);

        opts.onError( _loc('Mobile application is unable to edit user information') );
	}

	,getCurrentUserName: function() {
		var context = this._getAuthContext();

		if( context && context.name ) {
			return context.name;
		};

		return null;
	}

	,getCurrentUserRoles: function() {
		var context = this._getAuthContext();

		if( context && context.roles ) {
			return context.roles;
		};

		return null;
	}

	,doRolesContainAdmin: function(roles) {
		var admin = false;

		if( roles && roles.length ) {
			if( $.inArray('_admin',roles) >= 0 ) {
				admin = true;
			};
			if( $.inArray('administrator',roles) >= 0 ) {
				admin = true;
			};
			if( typeof(n2atlas) === 'object'
			 && typeof(n2atlas.name) === 'string' ) {
				var dbAdmin = n2atlas.name + '_administrator';
				if( $.inArray(dbAdmin,roles) >= 0 ) {
					admin = true;
				};
			};
		};

		return admin;
	},

	/*
	 * Returns context obtained from connection information
	 */
	_getAuthContext: function() {
	    var context = {
	        name: this.connectionInfo.user
	        ,roles: []
	    };
		return context;
	}

	,isLoggedIn: function() {
		return true;
	}

	,createAuthWidget: function(opts_){
		var dispatchService = null;
		var customService = null;
		if( this.options.directory ){
			dispatchService = this.options.directory.dispatchService;
			customService = this.options.directory.customService;
		};

		var widgetOptions = {
			elemId: null
			,elem: null
			,authService: this
			,dispatchService: dispatchService
		};

		if( customService ){
			var label = customService.getOption('authWidgetLoginLabel');
			if( label ) widgetOptions.labelLogin = label;

			label = customService.getOption('authWidgetLogoutLabel');
			if( label ) widgetOptions.labelLogout = label;

			label = customService.getOption('authWidgetWelcomeLabel');
			if( label ) widgetOptions.labelWelcome = label;
		};

		var opts = $n2.extend(widgetOptions,opts_);

		return new AuthWidget(opts);
	}

	,_getDispatcher: function(){
		var d = null;
		if( this.options.directory ){
			d = this.options.directory.dispatchService;
		};
		return d;
	}

	,_getUserService: function(){
		var us = null;
		if( this.options.directory ){
			us = this.options.directory.userService;
		};
		return us;
	}

	,_getRequestService: function(){
		var rs = null;
		if( this.options.directory ){
			rs = this.options.directory.requestService;
		};
		return rs;
	}

	,_dispatch: function(m){
		var dispatcher = this._getDispatcher();
		if( dispatcher ){
			var h = dispatcher.getHandle('n2.couchAuth');
			dispatcher.send(h,m);
		};
	},

	_handleEvent: function(m){
		if( m && m.type === 'login' ){
			this.login({
				username: m.username
				,password: m.password
			});

		} else if( m && m.type === 'loginShowForm' ){
			this.showLoginForm();

		} else if( m && m.type === 'logout' ){
			this.logout();

		} else if( m && m.type === 'authIsLoggedIn' ){
			// Synchronous call
			if( this.isLoggedIn() ){
				var context = this._getAuthContext();

				m.isLoggedIn = true;
				m.context = context;
				//m.userDoc = this.currentUserDoc;
			};
		};
	}

	,_getCustomService: function(){
		var cs = null;
		if( this.options.directory ){
			cs = this.options.directory.customService;
		};
		return cs;
	}

	,_shouldDisableCreateUserButton: function(){
		var flag = this.options.disableCreateUserButton;

		var customService = this._getCustomService();
		if( customService && !flag ){
			var o = customService.getOption('disableCreateUserButton');
			if( typeof(o) !== 'undefined' ){
				flag = o;
			};
		};

		return flag;
	}
});

//===================================================================================

var AuthWidget = $n2.Class({

	authService: null

	,elemId: null

	,currentUserName: null

	,currentUserDisplay: null

	,labelLogin: null

	,labelLogout: null

	,labelWelcome: null

	,initialize: function(options_){
		var opts = $n2.extend({
			elemId: null
			,elem: null
			,authService: null
			,dispatchService: null
			,labelLogin: null
			,labelLogout: null
			,labelWelcome: null
		},options_);

		var _this = this;

		this.authService = opts.authService;
		this.dispatchService = opts.dispatchService;
		this.labelLogin = opts.labelLogin;
		this.labelLogout = opts.labelLogout;
		this.labelWelcome = opts.labelWelcome;

		this.elemId = opts.elemId;
		if( !this.elemId && opts.elem ){
			this.elemId = $n2.utils.getElementIdentifier(opts.elem);
		};

		if( this.dispatchService ){
			var f = function(m, addr, d){
				_this._handleDispatch(m, addr, d);
			};
			this.dispatchService.register(DH,'authLoggedIn',f);
			this.dispatchService.register(DH,'authLoggedOut',f);
			this.dispatchService.register(DH,'authCurrentUserDoc',f);
			this.dispatchService.register(DH,'userInfo',f);
			this.dispatchService.register(DH,'userDocument',f);

			// Initialize State
			var m = {
				type: 'authIsLoggedIn'
				,isLoggedIn: false
			};
			this.dispatchService.synchronousCall(DH,m);
			if( m.isLoggedIn ){
				if( m.context && m.context.name ){
					this.currentUserName = m.context.name;
				};
				if( m.userDoc && m.userDoc.display ){
					this.currentUserDisplay = m.userDoc.display;
				};

			} else {
				this.currentUserName = null;
				this.currentUserDisplay = null;
			};

			this._loginStateChanged();
		};
	}

	,_getElem: function(){
		return $('#'+this.elemId);
	}

	,_handleDispatch: function(m, addr, dispatcher){
		// Check if widget still displayed
		var $elem = this._getElem();
		if( $elem.length < 1 ){
			// Deregister
			dispatcher.deregister(addr);
			return;
		};

		if( 'authLoggedIn' === m.type ){
			var ctxt = m.user;
			if( this.currentUserName !== ctxt.name ){
				this.currentUserName = ctxt.name;
				this.currentUserDisplay = null;

				this._loginStateChanged();
			};

		} else if( 'authLoggedOut' === m.type ){
			this.currentUserName = null;
			this.currentUserDisplay = null;
			this._loginStateChanged();

		} else if( 'authCurrentUserDoc' === m.type ){
			var userDoc = m.userDoc;
			if( userDoc
			 && userDoc.display !== this.currentUserDisplay ){
				this.currentUserDisplay = userDoc.display;
				this._loginStateChanged();
			};

		} else if( 'userInfo' === m.type ){
			if( m.userInfo
			 && this.currentUserName === m.userInfo.name ){
				this.currentUserDisplay = m.userInfo.display;
				this._loginStateChanged();
			};

		} else if( 'userDocument' === m.type ){
			if( m.userDoc
			 && this.currentUserName === m.userDoc.name ){
				this.currentUserDisplay = m.userDoc.display;
				this._loginStateChanged();
			};
		};
	}

	,_loginStateChanged: function() {

		var $login = this._getElem();
		if( $login.length < 1 ) return;
		$login.empty();

		var authService = this.authService;
		if( authService ) {
			var href = null;
			var displayName = null;
			var buttonText = null;
			var clickFn = null;
			var greetingFn = null;
			var greetingClass = null;
			if( this.currentUserName ){
				href = 'javascript:Logout';
				displayName = this.currentUserDisplay;
				if( !displayName ) displayName = this.currentUserName;
				greetingClass = 'nunaliit_login_greeting_name';
				buttonText = this.labelLogout ? this.labelLogout : _loc('Logout');
				clickFn = function(){
					authService.logout();
					return false;
				};
				greetingFn = function(){
					authService.editUser({
						userName: null // current user
					});
					return false;
				};
			} else {
				href = 'javascript:Login';
				displayName = this.labelWelcome ? this.labelWelcome : _loc('Welcome');
				greetingClass = 'nunaliit_login_greeting_welcome';
				buttonText = this.labelLogin ? this.labelLogin : _loc('Login');
				clickFn = function(){
					authService.showLoginForm();
					return false;
				};
			};

			var aElem = $('<a class="nunaliit_login_link"></a>')
				.attr("href",href)
				.text(buttonText);
			var linkInnerContainer = $('<div class="nunaliit_login_link_inner_container"></div>')
				.append(aElem);
			var linkOuterContainer = $('<div class="nunaliit_login_link_outer_container"></div>')
				.append(linkInnerContainer)
				.click(clickFn);

			var nameElem = $('<span></span>')
				.text(displayName);
			var greetingInner = $('<div class="nunaliit_login_greeting_inner_container"></div>')
				.append(nameElem);
			var greetingOuter = $('<div class="nunaliit_login_greeting_outer_container"></div>')
				.addClass(greetingClass)
				.append(greetingInner);
			if( greetingFn ){
				greetingOuter
					.addClass('nunaliit_login_greeting_with_editor')
					.click(greetingFn);
			};

			$login.empty().append(greetingOuter).append(linkOuterContainer);
		};
	}
});

//===================================================================================

$n2.cordovaAuth = {
	AuthService: AuthService
	,AuthWidget: AuthWidget
};

})(jQuery,nunaliit2);

