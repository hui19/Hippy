const ContextifyModule = internalBinding('ContextifyModule');

global.dynamicLoad = (path, cb) => {
  console.log(`global.__HIPPYCURDIR__ = ${global.__HIPPYCURDIR__}, path = ${path}`);
  ContextifyModule.LoadUriContent(global.__HIPPYCURDIR__ + path, cb);
};
