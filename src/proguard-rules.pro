# Add any ProGuard configurations specific to this
# extension here.

-keep public class com.watermelonice.wakeonlan.WakeOnLan {
    public *;
 }
-keeppackagenames gnu.kawa**, gnu.expr**

-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'com/watermelonice/wakeonlan/repack'
-flattenpackagehierarchy
-dontpreverify
