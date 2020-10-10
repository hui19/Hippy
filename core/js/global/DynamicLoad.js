const ContextifyModule = internalBinding('ContextifyModule');

global.dynamicLoad = (path, cb) => {
  ContextifyModule.LoadUriContent(path, cb);
};
