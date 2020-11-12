#ifndef LOADER_FILE_LOADER_H_
#define LOADER_FILE_LOADER_H_

#include "loader/adr-loader.h"
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

#endif  // LOADER_FILE_LOADER_H_
