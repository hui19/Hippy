#include "util.h"

namespace util {

bool has_uri_field(const http_parser_url &u, http_parser_url_fields field) {
  return u.field_set & (1 << field);
}

std::string get_uri_field(const char *uri,
                          const http_parser_url &u,
                          http_parser_url_fields field) {
  if (!util::has_uri_field(u, field)) {
    return "";
  }

  return std::string{uri + u.field_data[field].off, u.field_data[field].len};
}

}  // namespace util
