/* config.h for the Android build of Chocolate Heretic.
 * Hand-written replacement for the autotools/cmake generated header. */

#ifndef CHOCOLATE_HERETIC_CONFIG_H
#define CHOCOLATE_HERETIC_CONFIG_H

#define PACKAGE_NAME "Chocolate Heretic"
#define PACKAGE_TARNAME "chocolate-heretic"
#define PACKAGE_VERSION "3.1.1"
#define PACKAGE_STRING "Chocolate Heretic 3.1.1"
#define PROGRAM_PREFIX "chocolate-"

/* Platform capabilities (Android/Linux) */
#define HAVE_DIRENT_H 1
#define HAVE_MMAP 1
#define HAVE_DECL_STRCASECMP 1
#define HAVE_DECL_STRNCASECMP 1

#endif
