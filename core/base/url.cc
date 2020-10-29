#include "url.h"

#include <unordered_map>

#include "core/base/macros.h"

namespace hippy {
namespace base {

const char kEOL = -1;

static const std::unordered_map<std::string, int> kSpecialScheme = {
    {"ftp:", 21},    {"file:", -1}, {"http:", 80},
    {"https:", 443}, {"ws:", 80},   {"wss:", 443}};

inline bool IsSpecialScheme(const std::string& scheme) {
  if (kSpecialScheme.find(scheme) != kSpecialScheme.end()) {
    return true;
  }
  return false;
};

inline int NormalizePort(const std::string& scheme, int p) {
  auto it = kSpecialScheme.find(scheme);
  if (it != kSpecialScheme.end()) {
    if (p == it->second) {
      return -1;
    }
  }
  return p;
}

void Url::CopyBaseUrl(std::shared_ptr<Url> base, int flag) {
  if ((base->flag_ & URL_FLAGS_HAS_USERNAME) && (flag & kCopyUserName)) {
    flag_ |= URL_FLAGS_HAS_USERNAME;
    username_ = base->username_;
  }
  if ((base->flag_ & URL_FLAGS_HAS_PASSWORD) && (flag & kCopyPassword)) {
    flag_ |= URL_FLAGS_HAS_PASSWORD;
    password_ = base->password_;
  }
  if ((base->flag_ & URL_FLAGS_HAS_HOST) && (flag & kCopyHost)) {
    flag_ |= URL_FLAGS_HAS_HOST;
    host_ = base->host_;
  }
  if ((base->flag_ & URL_FLAGS_HAS_QUERY) && (flag & kCopyQuery)) {
    flag_ |= URL_FLAGS_HAS_QUERY;
    query_ = base->query_;
  }
  if ((base->flag_ & URL_FLAGS_HAS_PATH) && (flag & kCopyPath)) {
    flag_ |= URL_FLAGS_HAS_PATH;
    path_ = base->path_;
  }
  port_ = base->port_;
}

inline bool IsWindowsDriveLetter(char ch1, char ch2) {
  return std::isalpha(ch1) && (ch2 == ':' || ch2 == '|');
}

inline bool IsNormalizedWindowsDriveLetter(const std::string& str) {
  return str.size() >= 2 && std::isalpha(str[0]) && str[1] == ':';
}

inline bool StartsWithWindowsDriveLetter(char ch1, char ch2, size_t len) {
  return len >= 2 && IsWindowsDriveLetter(ch1, ch2) &&
         (len == 2 || ch2 == '/' || ch2 == '\\' || ch2 == '?' || ch2 == '#');
}

// Single dot segment can be ".", "%2e", or "%2E"
inline bool IsSingleDotSegment(const std::string& str) {
  switch (str.size()) {
    case 1:
      return str == ".";
    case 3:
      return str[0] == '%' && str[1] == '2' && std::tolower(str[2]) == 'e';
    default:
      return false;
  }
}

// Double dot segment can be:
//   "..", ".%2e", ".%2E", "%2e.", "%2E.",
//   "%2e%2e", "%2E%2E", "%2e%2E", or "%2E%2e"
inline bool IsDoubleDotSegment(const std::string& str) {
  switch (str.size()) {
    case 2:
      return str == "..";
    case 4:
      if (str[0] != '.' && str[0] != '%') {
        return false;
      }
      return ((str[0] == '.' && str[1] == '%' && str[2] == '2' &&
               std::tolower(str[3]) == 'e') ||
              (str[0] == '%' && str[1] == '2' && std::tolower(str[2]) == 'e' &&
               str[3] == '.'));
    case 6:
      return (str[0] == '%' && str[1] == '2' && std::tolower(str[2]) == 'e' &&
              str[3] == '%' && str[4] == '2' && std::tolower(str[5]) == 'e');
    default:
      return false;
  }
}

void Url::Parse(std::shared_ptr<Url> base) {
  const bool has_state_override = state_ != kUnknownState;
  enum URL_PARSE_STATE state = has_state_override ? state_ : kSchemeStart;

  if (state < kSchemeStart || state > kFragment) {
    flag_ |= URL_FLAGS_INVALID_PARSE_STATE;
    return;
  }

  std::string url(orig_);
  url.erase(std::remove_if(orig_.begin(), orig_.end(), [](unsigned char ch) {
    return std::iscntrl(ch) || std::isspace(ch);
  }));

  bool special = (flag_ & URL_FLAGS_SPECIAL);
  std::string buffer;
  bool cannot_be_base;

  bool at_flag = false;                   // Set when @ has been seen.
  bool square_bracket_flag = false;       // Set inside of [...]
  bool password_token_seen_flag = false;  // Set after a : after an username.

  auto it = url.begin();
  while(it != url.end()) {
    unsigned char ch = *it;
    const bool special_back_slash = (special && ch == '\\');
    switch (state_) {
      case kSchemeStart: {
        if (std::isalpha(ch)) {
          buffer += ch;
        } else if (!has_state_override) {
          state = kNoScheme;
          continue;
        } else {
          flag_ |= URL_FLAGS_FAILED;
          return;
        }
        break;
      }
      case kScheme: {
        if (std::isalnum(ch) || ch == '+' || ch == '-' || ch == '.') {
          buffer += std::tolower(ch);
        } else if (ch == ':' || (has_state_override && (it + 1) == url.end())) {
          if (has_state_override && buffer.size() == 0) {
            flag_ |= URL_FLAGS_TERMINATED;
            return;
          }
          buffer += ':';

          bool new_is_special = IsSpecialScheme(buffer);

          if (has_state_override) {
            if ((special != new_is_special) ||
                ((buffer == "file:") &&
                 ((flag_ & URL_FLAGS_HAS_USERNAME) ||
                  (flag_ & URL_FLAGS_HAS_PASSWORD) || (port_ != -1)))) {
              flag_ |= URL_FLAGS_TERMINATED;
              return;
            }
          }

          scheme_ = std::move(buffer);
          port_ = NormalizePort(scheme_, port_);
          if (new_is_special) {
            flag_ |= URL_FLAGS_SPECIAL;
            special = true;
          } else {
            flag_ &= ~URL_FLAGS_SPECIAL;
            special = false;
          }
          buffer.clear();
          if (has_state_override) {
            return;
          }
          if (scheme_ == "file:") {
            state = kFile;
          } else if (special && base && scheme_ == base->scheme_) {
            state = kSpecialRelativeOrAuthority;
          } else if (special) {
            state = kSpecialAuthoritySlashes;
          } else if (it + 1 != url.end() && *(it + 1) == '/') {
            state = kPathOrAuthority;
            ++it;
          } else {
            flag_ |= URL_FLAGS_CANNOT_BE_BASE;
            flag_ |= URL_FLAGS_HAS_PATH;
            path_.emplace_back("");
            state = kCannotBeBase;
          }
        } else if (!has_state_override) {
          buffer.clear();
          state = kNoScheme;
          it = url.begin();
          continue;
        } else {
          flag_ |= URL_FLAGS_FAILED;
          return;
        }
        break;
      }
      case kNoScheme: {
        cannot_be_base = base && (base->flag_ & URL_FLAGS_CANNOT_BE_BASE);
        if (!base || (cannot_be_base && ch != '#')) {
          flag_ |= URL_FLAGS_FAILED;
          return;
        } else if (cannot_be_base && ch == '#') {
          scheme_ = base->scheme_;
          if (IsSpecialScheme(scheme_)) {
            flag_ |= URL_FLAGS_SPECIAL;
            special = true;
          } else {
            flag_ &= ~URL_FLAGS_SPECIAL;
            special = false;
          }
          if (base->flag_ & URL_FLAGS_HAS_PATH) {
            flag_ |= URL_FLAGS_HAS_PATH;
            path_ = base->path_;
          }
          if (base->flag_ & URL_FLAGS_HAS_QUERY) {
            flag_ |= URL_FLAGS_HAS_QUERY;
            query_ = base->query_;
          }
          if (base->flag_ & URL_FLAGS_HAS_FRAGMENT) {
            flag_ |= URL_FLAGS_HAS_FRAGMENT;
            fragment_ = base->fragment_;
          }
          flag_ |= URL_FLAGS_CANNOT_BE_BASE;
          state = kFragment;
        } else if (base && base->scheme_ != "file:") {
          state = kRelative;
          continue;
        } else {
          scheme_ = "file:";
          flag_ |= URL_FLAGS_SPECIAL;
          special = true;
          state = kFile;
          continue;
        }
        break;
      }
      case kSpecialRelativeOrAuthority: {
        if (*it == '/' && (it + 1) != url.end() && *(it + 1) == '/') {
          state = kSpecialAuthorityIgnoreSlashes;
          ++it;
        } else {
          state = kRelative;
          continue;
        }
        break;
      }
      case kPathOrAuthority: {
        if (ch == '/') {
          state = kAuthority;
        } else {
          state = kPath;
          continue;
        }
        break;
      }
      case kRelative: {
        scheme_ = base->scheme_;
        if (IsSpecialScheme(scheme_)) {
          flag_ |= URL_FLAGS_SPECIAL;
          special = true;
        } else {
          flag_ &= ~URL_FLAGS_SPECIAL;
          special = false;
        }
        if (it + 1 == url.end()) {
          CopyBaseUrl(base, kCopyALL);
        } else {
          switch (ch) {
            case '/':
              state = kRelativeSlash;
              break;
            case '?':
              CopyBaseUrl(base, kCopyNotQuery);
              state = kQuery;
              break;
            case '#':
              CopyBaseUrl(base, kCopyALL);
              state = kFragment;
              break;
            default:
              if (special_back_slash) {
                state = kRelativeSlash;
              } else {
                CopyBaseUrl(base, kCopyALL);
                if (base->flag_ & URL_FLAGS_HAS_PATH) {
                  // ShortenUrlPath(url); todo
                }
                state = kPath;
                continue;
              }
          }
        }
        break;
      }
      case kRelativeSlash: {
        if (IsSpecialScheme(scheme_) && (ch == '/' || ch == '\\')) {
          state = kSpecialAuthorityIgnoreSlashes;
        } else if (ch == '/') {
          state = kAuthority;
        } else {
          CopyBaseUrl(base, kCopyHost | kCopyPassword | kCopyPassword);
          state = kPath;
          continue;
        }
        break;
      }
      case kSpecialAuthoritySlashes: {
        state = kSpecialAuthorityIgnoreSlashes;
        if (ch == '/' && it + 1 != url.end() && *(it + 1) == '/') {
          ++it;
        } else {
          continue;
        }
        break;
      }
      case kSpecialAuthorityIgnoreSlashes: {
        if (ch != '/' && ch != '\\') {
          state = kAuthority;
          continue;
        }
        break;
      }
      case kAuthority: {
        if (ch == '@') {
          if (at_flag) {
            buffer.reserve(buffer.size() + 3);
            buffer.insert(0, "%40");  // @ = "%40"
          }
          at_flag = true;
          const size_t len = buffer.size();
          if (len > 0 && buffer[0] != ':') {
            flag_ |= URL_FLAGS_HAS_USERNAME;
          }
          for (size_t n = 0; n < len; n++) {
            const char ch = buffer[n];
            if (ch == ':') {
              flag_ |= URL_FLAGS_HAS_PASSWORD;
              if (!password_token_seen_flag) {
                password_token_seen_flag = true;
                continue;
              }
            }
            if (password_token_seen_flag) {
              AppendOrEscape(&password_, ch, USERINFO_ENCODE_SET);
            } else {
              AppendOrEscape(&username_, ch, USERINFO_ENCODE_SET);
            }
          }
          buffer.clear();
        } else if ((it + 1) == url.end() || ch == '/' || ch == '?' || ch == '#' ||
                   special_back_slash) {
          if (at_flag && buffer.size() == 0) {
            flag_ |= URL_FLAGS_FAILED;
            return;
          }
          it -= buffer.size() + 1;
          buffer.clear();
          state = kHost;
        } else {
          buffer += ch;
        }
        break;
      }
      case kHost:
      case kHostname: {
        if (has_state_override && scheme_ == "file:") {
          state = kFileHost;
          continue;
        } else if (ch == ':' && !square_bracket_flag) {
          if (buffer.size() == 0) {
            flag_ |= URL_FLAGS_FAILED;
            return;
          }
          flag_ |= URL_FLAGS_HAS_HOST;
          URLHost host;
          if (host.ParseHost(buffer, special)) {
            flag_ |= URL_FLAGS_FAILED;
            return;
          } else {
            host_ = host.ToStringMove();
          }
          buffer.clear();
          state = kPort;
          if (state_ == kHostname) {
            return;
          }
        } else if ((it + 1) == url.end() || ch == '/' || ch == '?' || ch == '#' || special_back_slash) {
          --it;
          if (special && buffer.size() == 0) {
            flag_ |= URL_FLAGS_FAILED;
            return;
          }
          if (has_state_override && buffer.size() == 0 &&
              ((username_.size() > 0 || password_.size() > 0) || port_ != -1)) {
            flag_ |= URL_FLAGS_TERMINATED;
            return;
          }
          flag_ |= URL_FLAGS_HAS_HOST;
          URLHost host;
          if (host.ParseHost(buffer, special)) {
            host_ = host.ToStringMove();
          } else {
            flag_ |= URL_FLAGS_FAILED;
            return;
          }
          buffer.clear();
          state = kPathStart;
          if (has_state_override) {
            return;
          }
        } else {
          if (ch == '[') {
            square_bracket_flag = true;
          } 
          if (ch == ']') {
            square_bracket_flag = false;
          }  
          buffer += ch;
        }
        break;
      }
      case kPort: {
        if (std::isdigit(ch)) {
          buffer += ch;
        } else if (has_state_override || (it + 1) == url.end() || ch == '/' || ch == '?' ||
                   ch == '#' || special_back_slash) {
          if (buffer.size() > 0) {
            unsigned port = 0;
            // the condition port <= 0xffff prevents integer overflow
            for (size_t i = 0; port <= 0xffff && i < buffer.size(); ++i) {
              port = port * 10 + buffer[i] - '0';
            }
            if (port > 0xffff) {
              // TODO(TimothyGu): This hack is currently needed for the host
              // setter since it needs access to hostname if it is valid, and
              // if the FAILED flag is set the entire response to JS layer
              // will be empty.
              if (state_ == kHost) {
                port_ = -1;
              } else {
                flag_ |= URL_FLAGS_FAILED;
              }
              return;
            }
            // the port is valid
            port_ = NormalizePort(scheme_, static_cast<int>(port));
            if (port_ == -1) {
              flag_ |= URL_FLAGS_IS_DEFAULT_SCHEME_PORT;
            }
            buffer.clear();
          } else if (has_state_override) {
            // TODO(TimothyGu): Similar case as above.
            if (state_ == kHost) {
              port_ = -1;
            } else {
              flag_ |= URL_FLAGS_TERMINATED;
            }
            return;
          }
          state = kPathStart;
          continue;
        } else {
          flag_ |= URL_FLAGS_FAILED;
          return;
        }
        break;
      }
      case kFile: {
        scheme_ = "file:";
        if (ch == '/' || ch == '\\') {
          state = kFileSlash;
        } else if (base && base->scheme_ == "file:") {
          switch (ch) {
            case kEOL:
              if (base->flag_ & URL_FLAGS_HAS_HOST) {
                flag_ |= URL_FLAGS_HAS_HOST;
                host_ = base->host_;
              }
              if (base->flag_ & URL_FLAGS_HAS_PATH) {
                flag_ |= URL_FLAGS_HAS_PATH;
                path_ = base->path_;
              }
              if (base->flag_ & URL_FLAGS_HAS_QUERY) {
                flag_ |= URL_FLAGS_HAS_QUERY;
                query_ = base->query_;
              }
              break;
            case '?':
              if (base->flag_ & URL_FLAGS_HAS_HOST) {
                flag_ |= URL_FLAGS_HAS_HOST;
                host_ = base->host_;
              }
              if (base->flag_ & URL_FLAGS_HAS_PATH) {
                flag_ |= URL_FLAGS_HAS_PATH;
                path_ = base->path_;
              }
              flag_ |= URL_FLAGS_HAS_QUERY;
              query_.clear();
              state = kQuery;
              break;
            case '#':
              if (base->flag_ & URL_FLAGS_HAS_HOST) {
                flag_ |= URL_FLAGS_HAS_HOST;
                host_ = base->host_;
              }
              if (base->flag_ & URL_FLAGS_HAS_PATH) {
                flag_ |= URL_FLAGS_HAS_PATH;
                path_ = base->path_;
              }
              if (base->flag_ & URL_FLAGS_HAS_QUERY) {
                flag_ |= URL_FLAGS_HAS_QUERY;
                query_ = base->query_;
              }
              flag_ |= URL_FLAGS_HAS_FRAGMENT;
              fragment_.clear();
              state = kFragment;
              break;
            default:
              if (it + 1 != url.end() &&
                  !StartsWithWindowsDriveLetter(*it, *(it + 1), url.length())) {
                if (base->flag_ & URL_FLAGS_HAS_HOST) {
                  flag_ |= URL_FLAGS_HAS_HOST;
                  host_ = base->host_;
                }
                if (base->flag_ & URL_FLAGS_HAS_PATH) {
                  flag_ |= URL_FLAGS_HAS_PATH;
                  path_ = base->path_;
                }
                // ShortenUrlPath(url);
              }
              state = kPath;
              continue;
          }
        } else {
          state = kPath;
          continue;
        }
        break;
      }
      case kFileSlash: {
        if (ch == '/' || ch == '\\') {
          state = kFileHost;
        } else {
          if (base && base->scheme_ == "file:" && it + 1 != url.end() &&
              !StartsWithWindowsDriveLetter(*it, *(it + 1), url.length())) {
            if (IsNormalizedWindowsDriveLetter(base->path_[0])) {
              flag_ |= URL_FLAGS_HAS_PATH;
              path_.push_back(base->path_[0]);
            } else {
              if (base->flag_ & URL_FLAGS_HAS_HOST) {
                flag_ |= URL_FLAGS_HAS_HOST;
                host_ = base->host_;
              } else {
                flag_ &= ~URL_FLAGS_HAS_HOST;
                host_.clear();
              }
            }
          }
          state = kPath;
          continue;
        }
        break;
      }
      case kFileHost: {
        if (ch == kEOL || ch == '/' || ch == '\\' || ch == '?' || ch == '#') {
          if (!has_state_override && buffer.size() == 2 &&
              IsWindowsDriveLetter(buffer[0], buffer[1])) {
            state = kPath;
          } else if (buffer.size() == 0) {
            flag_ |= URL_FLAGS_HAS_HOST;
            host_.clear();
            if (has_state_override) {
              return;
            }

            state = kPathStart;
          } else {
            URLHost host;
            if (host.ParseHost(buffer, special)) {
              flag_ |= URL_FLAGS_FAILED;
              return;
            } else {
              host_ = host.ToStringMove();
            }
            URLHost host;
            if (host.ParseHost(buffer, special)) {
              flag_ |= URL_FLAGS_FAILED;
              return;
            }
            std::string host_str = host.ToStringMove();
            if (host_str == "localhost") {
              host_str.clear();
            }
            flag_ |= URL_FLAGS_HAS_HOST;
            host_ = host_str;
            if (has_state_override) {
              return;
            }
            buffer.clear();
            state = kPathStart;
          }
          continue;
        } else {
          buffer += ch;
        }
        break;
      }
      case kPathStart: {
        if (IsSpecialScheme(scheme_)) {
          state = kPath;
          if (ch != '/' && ch != '\\') {
            continue;
          }
        } else if (!has_state_override && ch == '?') {
          flag_ |= URL_FLAGS_HAS_QUERY;
          query_.clear();
          state = kQuery;
        } else if (!has_state_override && ch == '#') {
          flag_ |= URL_FLAGS_HAS_FRAGMENT;
          fragment_.clear();
          state = kFragment;
        } else if (ch != kEOL) {
          state = kPath;
          if (ch != '/') {
            continue;
          }
        }
        break;
      }
      case kPath: {
        if (ch == kEOL || ch == '/' || special_back_slash ||
            (!has_state_override && (ch == '?' || ch == '#'))) {
          if (IsDoubleDotSegment(buffer)) {
            // ShortenUrlPath(url);
            if (ch != '/' && !special_back_slash) {
              flag_ |= URL_FLAGS_HAS_PATH;
              path_.emplace_back("");
            }
          } else if (IsSingleDotSegment(buffer) && ch != '/' &&
                     !special_back_slash) {
            flag_ |= URL_FLAGS_HAS_PATH;
            path_.emplace_back("");
          } else if (!IsSingleDotSegment(buffer)) {
            if (scheme_ == "file:" && path_.empty() && buffer.size() == 2 &&
                IsWindowsDriveLetter(buffer[0], buffer[1])) {
              if ((flag_ & URL_FLAGS_HAS_HOST) && !host_.empty()) {
                host_.clear();
                flag_ |= URL_FLAGS_HAS_HOST;
              }
              buffer[1] = ':';
            }
            flag_ |= URL_FLAGS_HAS_PATH;
            path_.emplace_back(std::move(buffer));
          }
          buffer.clear();
          if (scheme_ == "file:" && (ch == kEOL || ch == '?' || ch == '#')) {
            while (path_.size() > 1 && path_[0].length() == 0) {
              path_.erase(path_.begin());
            }
          }
          if (ch == '?') {
            flag_ |= URL_FLAGS_HAS_QUERY;
            state = kQuery;
          } else if (ch == '#') {
            state = kFragment;
          }
        } else {
          AppendOrEscape(&buffer, ch, PATH_ENCODE_SET);
        }
        break;
      }
      case kCannotBeBase: {
        switch (ch) {
          case '?':
            state = kQuery;
            break;
          case '#':
            state = kFragment;
            break;
          default:
            if (path_.size() == 0) {
              path_.emplace_back("");
            }
            if (path_.size() > 0 && ch != kEOL) {
              AppendOrEscape(&path_[0], ch, C0_CONTROL_ENCODE_SET);
            }
        }
        break;
      }
      case kQuery: {
        if (ch == kEOL || (!has_state_override && ch == '#')) {
          flag_ |= URL_FLAGS_HAS_QUERY;
          query_ = std::move(buffer);
          buffer.clear();
          if (ch == '#')
            state = kFragment;
        } else {
          AppendOrEscape(
              &buffer, ch,
              special ? QUERY_ENCODE_SET_SPECIAL : QUERY_ENCODE_SET_NONSPECIAL);
        }
        break;
      }
      case kFragment: {
        switch (ch) {
          case kEOL:
            flag_ |= URL_FLAGS_HAS_FRAGMENT;
            fragment_ = std::move(buffer);
            break;
          case 0:
            break;
          default:
            AppendOrEscape(&buffer, ch, FRAGMENT_ENCODE_SET);
        }
        break;
      }
      default: {
        flag_ |= URL_FLAGS_INVALID_PARSE_STATE;
        return;
      }
    }

    ++it;
  }
}

}  // namespace base
}  // namespace hippy
