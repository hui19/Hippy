#ifndef URL_PARSER_UTIL_H
#define URL_PARSER_UTIL_H

#include <string>

#include "url_parser.h"

namespace util {

inline bool has_uri_field(const http_parser_url &u,
                          http_parser_url_fields field);
std::string get_uri_field(const char *uri,
                          const http_parser_url &u,
                          http_parser_url_fields field);
}  // namespace util

#endif  // URL_PARSER_UTIL_H
