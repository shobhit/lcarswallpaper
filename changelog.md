## 1.x (trunk) ##
  * Still in development
  * Expect display glitches
  * Stability tested on Moto Droid BIONIC

## 1.1.1 ##
  * Landscape orientation should draw correctly (no flickering)
  * NOTE: not designed for tablets.  Landscape mode is boring and portrait mode is optimized for 9:16 (16:9 portrait) aspect.

## 1.1 ##
  * Fixes issue with Samsung launcher on Galaxy S (2.2.1) not scrolling the LCARS wallpaper with the home screens (may also fix other phones with similar behavior)
  * A lot of code re-factoring to warrant a new minor release number

## 1.0.3 ##
  * Should work on different resolutions now
  * Deuterium electron animation thread now calculates next position only once per frame, gaining a few % of CPU when active
## 1.0.2 ##
  * Touch various areas of ship schematics to display description
  * Touch "SECURITY" to cycle ship schematics ((c) L Tanganho)
    * Galaxy class schematic
    * Excelsior class schematic
    * B'Rel class schematic
  * Implemented hashCode() to prevent display of duplicate services/apps
  * Moved several calculations into new threads, out of UI thread
  * Improved thread code (using java.util.concurrent - thanks Dave!)
  * CAUTION graphic when battery hits 20%
  * larger font for deuterium status
  * faster frame rate when viewing deuterium status (better atom animation)
  * removed some dead code
  * ISSUE: Designed for 1.78 aspect ratio (e.g., 854x780).  Other screens may result in cropping of the LCARS wallpaper (i.e., Droid Incredible, EVO 4G, etc.)
## 1.0.1 ##
  * Deuterium (battery) status
  * Click "Warp EF" to switch between power and memory stats

## 1.0.0 ##
  * First release