const ContextifyModule = internalBinding('ContextifyModule');

global.dynamicLoad = (path, cb) => {
  ContextifyModule.LoadJsFile(path, cb);
};
