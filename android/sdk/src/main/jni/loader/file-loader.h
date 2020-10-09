#ifndef CORE_FILE_LOADER_H_
#define CORE_FILE_LOADER_H_

#include "adr-loader.h"
#include "scoped-java-ref.h"

class FileLoader : public ADRLoader {
 public:
  FileLoader(const std::string& base_path);
  virtual ~FileLoader(){};
  
  virtual std::string Load(const std::string& uri);
  virtual std::unique_ptr<std::vector<char>> LoadBytes(const std::string& uri);

 private:
  bool CheckValid(const std::string& path);
  std::string base_path_;
};

#endif  // CORE_FILE_LOADER_H_
