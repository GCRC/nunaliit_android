/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/
cordova.define("org.apache.cordova.file.FileSystem", function(require, exports, module) {
var DirectoryEntry = require('./DirectoryEntry');
FILESYSTEM_PROTOCOL = 'cdvfile'; // eslint-disable-line no-undef

/**
 * An interface representing a file system
 *
 * @constructor
 * {DOMString} name the unique name of the file system (readonly)
 * {DirectoryEntry} root directory of the file system (readonly)
 */
var FileSystem = function (name, root) {
    this.name = name;
    if (root) {
        this.root = new DirectoryEntry(root.name, root.fullPath, this, root.nativeURL);
    } else {
        this.root = new DirectoryEntry(this.name, '/', this);
    }
};

FileSystem.prototype.__format__ = function (fullPath, nativeUrl) {
    var path;
    var contentUrlMatch = /^content:\/\//.exec(nativeUrl);
    if (contentUrlMatch) {
        // When available, use the path from a native content URL, which was already encoded by Android.
        // This is necessary because JavaScript's encodeURI() does not encode as many characters as
        // Android, which can result in permission exceptions when the encoding of a content URI
        // doesn't match the string for which permission was originally granted.
        path = nativeUrl.substring(contentUrlMatch[0].length - 1);
    } else {
        path = FileSystem.encodeURIPath(fullPath); // eslint-disable-line no-undef
        if (!/^\//.test(path)) {
            path = '/' + path;
        }

        var m = /\?.*/.exec(nativeUrl);
        if (m) {
            path += m[0];
        }
    }

    return FILESYSTEM_PROTOCOL + '://localhost/' + this.name + path; // eslint-disable-line no-undef
};

FileSystem.prototype.toJSON = function () {
    return '<FileSystem: ' + this.name + '>';
};

// Use instead of encodeURI() when encoding just the path part of a URI rather than an entire URI.
FileSystem.encodeURIPath = function (path) {
    // Because # is a valid filename character, it must be encoded to prevent part of the
    // path from being parsed as a URI fragment.
    return encodeURI(path).replace(/#/g, '%23');
};

module.exports = FileSystem;
});