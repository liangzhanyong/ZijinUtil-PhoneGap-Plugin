function ZijinUtil() {
}

ZijinUtil.prototype.openScanReceiver = function(options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'ZijinUtil', 'openScanReceiver', [options]);
}

ZijinUtil.prototype.closeScanReceiver = function() {
  cordova.exec(null, null, 'ZijinUtil', 'closeScanReceiver', []);
}

ZijinUtil.prototype.startInventoryReal = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'ZijinUtil', 'startInventoryReal', []);
}

ZijinUtil.prototype.stopInventoryReal = function() {
  cordova.exec(null, null, 'ZijinUtil', 'stopInventoryReal', []);
}

ZijinUtil.prototype.setOutputPower = function (mOutPower) {
  cordova.exec(null, null, "ZijinUtil", "setOutputPower", [{mOutPower: mOutPower}]);
};

ZijinUtil.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.zijinutil = new ZijinUtil();
  return window.plugins.zijinutil;
};

cordova.addConstructor(ZijinUtil.install);

