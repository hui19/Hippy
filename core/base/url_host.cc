#include "url_host.h"

#include <sstream>

#include "core/base/macros.h"
#include "core/base/logging.h"


namespace hippy {
namespace base {

URLHost::~URLHost() {
  Reset();
}

bool IsForbiddenHostCodePoint(char ch) {
  if (ch == '\0' || ch == '\t' || ch == '\n' || ch == '\r' || ch == ' ' ||
      ch == '#' || ch == '%' || ch == '/' || ch == ':' || ch == '?' ||
      ch == '@' || ch == '[' || ch == '\\' || ch == ']') {
    return true;
  }
  return false;
}

inline bool IsASCIIHexDigit(const char ch) {
  if (std::isdigit(ch) || (ch >= 'A' && ch <= 'F') ||
      (ch >= 'a' && ch <= 'f')) {
    return true;
  }

  return false;
}

inline unsigned hex2bin(const char ch) {
  if (ch >= '0' && ch <= '9')
    return ch - '0';
  if (ch >= 'A' && ch <= 'F')
    return 10 + (ch - 'A');
  if (ch >= 'a' && ch <= 'f')
    return 10 + (ch - 'a');
  return static_cast<unsigned>(-1);
}

inline std::string PercentDecode(const std::string& input) {
  std::string dest;
  if (input.size() == 0) {
    return dest;
  }
  auto it = input.begin();

  while (it != input.end()) {
    const char ch = *it;
    const auto remaining = input.end() - 1 - it;
    if (ch != '%' || remaining < 2 ||
        (ch == '%' &&
         (!IsASCIIHexDigit(*(it + 1)) || !IsASCIIHexDigit(*(it +2))))) {
      dest += ch;
      ++it;
      continue;
    } else {
      unsigned a = hex2bin(*(it + 1));
      unsigned b = hex2bin(*(it + 2));
      char c = static_cast<char>(a * 16 + b);
      dest += c;
      it += 3;
    }
  }
  return dest;
}
 

bool URLHost::ParseIPv4Host(const std::string& input) {
  if (!input.size()) {
    return false;
  }
    
  int parts = 0;
  uint32_t val = 0;
  uint64_t numbers[4];
  int tooBigNumbers = 0;

  std::string::size_type pre_pos = 0, pos = 0;
  while ((pos = input.find('.', pos)) != std::string::npos) {
    std::string sub(input.substr(pre_pos, pos - pre_pos));
    int64_t num = std::stoi(sub);
    if (num < 0) {
      return false;
    }
    if (num > 255) {
      ++tooBigNumbers;
    }
    numbers[parts++] = num;
    pre_pos = ++pos;
  }

  // If any but the last item in numbers is greater than 255, return failure.
  // If the last item in numbers is greater than or equal to
  // 256^(5 - the number of items in numbers), return failure.
  if (tooBigNumbers > 1 || (tooBigNumbers == 1 && numbers[parts - 1] <= 255) ||
      numbers[parts - 1] >= pow(256, static_cast<double>(5 - parts))) {
    return false;
  }

  type_ = HostType::H_IPV4;
  val = numbers[parts - 1];
  for (int n = 0; n < parts - 1; n++) {
    double b = 3 - n;
    val += numbers[n] * pow(256, b);
  }

  value_.ipv4 = val;
  return true;
}

bool URLHost::ParseIPv6Host(const std::string& input) {
  HIPPY_CHECK(type_ == HostType::H_FAILED);
  unsigned size = arraysize(value_.ipv6);
  for (unsigned i = 0; i < size; ++i) {
    value_.ipv6[i] = 0;
  }
   
  uint16_t* piece_pointer = &value_.ipv6[0];
  uint16_t* const buffer_end = piece_pointer + size;
  uint16_t* compress_pointer = nullptr;
  auto it = input.begin();
  unsigned value, len, numbers_seen;
  if (it != input.end()) {
    if (*it == ':') {
      if (input.size() < 2 || *(it + 1) != ':') {
        return false;
      } 
      it += 2;
      ++piece_pointer;
      compress_pointer = piece_pointer;
    }

    if (piece_pointer >= buffer_end) {
      return false;
    }

    while (it != input.end()) {
      if (*it == ':') {
        if (compress_pointer != nullptr) {
          return false;
        }

        ++it;
        ++piece_pointer;
        compress_pointer = piece_pointer;
        continue;
      }
      value = 0;
      len = 0;
      while (len < 4 && IsASCIIHexDigit(*it)) {
        value = value * 0x10 + hex2bin(*it);
        ++it;
        ++len;
      }
      switch (*it) {
        case '.':
          if (len == 0) {
            return false;
          }
          it -= len;
          if (piece_pointer > buffer_end - 2) {
            return false;
          }
          numbers_seen = 0;
          while (it != input.end()) {
            value = 0xffffffff;
            if (numbers_seen > 0) {
              if (*it == '.' && numbers_seen < 4) {
                ++it;
              } else {
                return false;
              }
            }
            if (!std::isdigit(*it))
              return false;
            while (std::isdigit(*it)) {
              unsigned number = *it - '0';
              if (value == 0xffffffff) {
                value = number;
              } else if (value == 0) {
                return false;
              } else {
                value = value * 10 + number;
              }
              if (value > 255) {
                return false;
              }
              ++it;
            }
            *piece_pointer = *piece_pointer * 0x100 + value;
            numbers_seen++;
            if (numbers_seen == 2 || numbers_seen == 4) {
              piece_pointer++;
            }
          }
          if (numbers_seen != 4) {
            return false;
          }
          continue;
        case ':':
          ++it;
          if (it == input.end()) {
            return false;
          }
          break;
        default:
          return false;
      }
      *piece_pointer = value;
      piece_pointer++;
    }
  }

  if (compress_pointer != nullptr) {
    unsigned swaps = piece_pointer - compress_pointer;
    piece_pointer = buffer_end - 1; 
    while (piece_pointer != &value_.ipv6[0] && swaps > 0) {
      uint16_t temp = *piece_pointer;
      uint16_t* swap_piece = compress_pointer + swaps - 1;
      *piece_pointer = *swap_piece;
      *swap_piece = temp;
      --piece_pointer;
      --swaps;
    }
  } else if (compress_pointer == nullptr && piece_pointer != buffer_end) {
    return false;
  }
  type_ = HostType::H_IPV6;
  return true;
}

bool URLHost::ParseOpaqueHost(const std::string& input) {
  std::string output;
  output.reserve(input.size());
  for (auto it = input.begin(); it < input.end(); ++it) {
    const char ch = *it;
    if (ch != '%' && IsForbiddenHostCodePoint(ch)) {
      return false;
    } else {
      AppendOrEscape(&output, ch, C0_CONTROL_ENCODE_SET);
    }
  }

  SetOpaque(std::move(output));
  return true;
}

bool URLHost::ParseHost(const std::string& input,
                        bool is_special) {
  HIPPY_CHECK(type_ == HostType::H_FAILED);
  auto it = input.begin();

  if (it == input.end()) {
    return false;
  }
  

  if (*it == '[') {
    if (*(input.rbegin()) != ']') {
      return false;
    }

    std::string str(++it, --input.end());
    return ParseIPv6Host(str);
  }

  if (!is_special) {
    return ParseOpaqueHost(input);
  }

  std::string decoded = PercentDecode(input);

  auto it = decoded.begin();
  while (it != decoded.end()) {
    if (IsForbiddenHostCodePoint(*it)) {
      return false;
    }
    ++it;
  }

  bool is_ipv4 = ParseIPv4Host(decoded);
  if (is_ipv4) {
    return true;
  }

  SetDomain(std::move(decoded));
  return true;
}

std::string URLHost::ToStringMove() {
  std::string return_value;
  switch (type_) {
    case HostType::H_DOMAIN:
    case HostType::H_OPAQUE:
      return_value = std::move(value_.domain_or_opaque);
      break;
    default:
      return_value = ToString();
      break;
  }
  Reset();
  return return_value;
}

inline const uint16_t* FindLongestZeroSequence(const uint16_t* values, size_t len) {
  const uint16_t* start = values;
  const uint16_t* end = start + len;
  const uint16_t* result = nullptr;

  const uint16_t* current = nullptr;
  unsigned counter = 0, longest = 1;

  while (start < end) {
    if (*start == 0) {
      if (current == nullptr) {
        current = start;
      }
      counter++;
    } else {
      if (counter > longest) {
        longest = counter;
        result = current;
      }
      counter = 0;
      current = nullptr;
    }
    start++;
  }
  if (counter > longest) {
    result = current;
  }  
  return result;
}

std::string URLHost::ToString() const {
  std::string dest;
  switch (type_) {
    case HostType::H_DOMAIN:
    case HostType::H_OPAQUE:
      return value_.domain_or_opaque;
      break;
    case HostType::H_IPV4: {
      dest.reserve(15);
      uint32_t value = value_.ipv4;
      for (int i = 0; i < 4; ++i) {
        std::string buf = std::to_string(value % 256);
        dest.insert(0, buf);
        if (i < 3) {
          dest.insert(0, 1, '.');
        }    
        value /= 256;
      }
      break;
    }
    case HostType::H_IPV6: {
      dest.reserve(41);
      dest += '[';
      const uint16_t* start = &value_.ipv6[0];
      const uint16_t* compress_pointer = FindLongestZeroSequence(start, 8);
      bool ignore0 = false;
      for (int i = 0; i <= 7; ++i) {
        const uint16_t* piece = &value_.ipv6[i];
        if (ignore0 && *piece == 0) {
          continue;
        } else if (ignore0) {
          ignore0 = false;
        }
         
        if (compress_pointer == piece) {
          dest += i == 0 ? "::" : ":";
          ignore0 = true;
          continue;
        }
        std::stringstream stream;
        stream << std::hex << *piece;
        dest += stream.str();
        if (i < 7) {
          dest += ':';
        }
      }
      dest += ']';
      break;
    }
    case HostType::H_FAILED:
      break;
  }
  return dest;
}

}
}  // namespace hippy
