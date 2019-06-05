function ZijinUtil() {
}

ZijinUtil.prototype.scan = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'ZijinUtil', 'scan', []);
}

ZijinUtil.prototype.continueScanning = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'ZijinUtil', 'continueScanning', []);
}

ZijinUtil.prototype.closeScanning = function() {
  cordova.exec(null, null, 'ZijinUtil', 'closeScanning', []);
}

ZijinUtil.prototype.setScanner = function(options) {
  cordova.exec(null, null, 'ZijinUtil', 'setScanner', [options]);
}

ZijinUtil.prototype.setScanInterval = function(time) {
  cordova.exec(null, null, 'ZijinUtil', 'setScanInterval', [{time: time}]);
}

ZijinUtil.prototype.openUHF = function () {
  cordova.exec(null, null, "ZijinUtil", "openUHF", []);
};

ZijinUtil.prototype.startInventoryReal = function (options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "startInventoryReal", [options]);
};

ZijinUtil.prototype.closeUHF = function () {
  cordova.exec(null, null, "ZijinUtil", "closeUHF", []);
};

ZijinUtil.prototype.stopInventoryReal = function () {
  cordova.exec(null, null, "ZijinUtil", "stopInventoryReal", []);
};

ZijinUtil.prototype.getReaderTemperature = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "getReaderTemperature", []);
};

ZijinUtil.prototype.killTag = function (options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "killTag", [options]);
};

ZijinUtil.prototype.lockTag = function (options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "lockTag", [options]);
};

ZijinUtil.prototype.readTag = function (options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "readTag", [options]);
};

ZijinUtil.prototype.writeTag = function (options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "writeTag", [options]);
};

ZijinUtil.prototype.reset = function () {
  cordova.exec(null, null, "ZijinUtil", "reset", [{}]);
};

ZijinUtil.prototype.setInventoryDelayMillis = function (delayMillis) {
  cordova.exec(null, null, "ZijinUtil", "setInventoryDelayMillis", [{delayMillis: delayMillis}]);
};

ZijinUtil.prototype.setOutputPower = function (mOutPower) {
  cordova.exec(null, null, "ZijinUtil", "setOutputPower", [{mOutPower: mOutPower}]);
};

ZijinUtil.prototype.openFingerprint = function () {
  cordova.exec(null, null, "ZijinUtil", "openFingerprint", [{}]);
};

ZijinUtil.prototype.closeFingerprint = function () {
  cordova.exec(null, null, "ZijinUtil", "closeFingerprint", [{}]);
};

ZijinUtil.prototype.scanFingerprint = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "scanFingerprint", [{}]);
};

ZijinUtil.prototype.verifyFingerprint = function (charArray, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ZijinUtil", "verifyFingerprint", [{chars: charArray.join('$')}]);
};

ZijinUtil.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.zijinutil = new ZijinUtil();
  return window.plugins.zijinutil;
};

cordova.addConstructor(ZijinUtil.install);

