/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Index the current firmware filenames; conflicting aliases are never guessed. */
public final class HomeHapticFirmwareNames {
    private HomeHapticFirmwareNames() { }
    private static final Pattern FILE=Pattern.compile("^(\\d+)_([A-Za-z][A-Za-z0-9_]*?)(?:_P)?_RTP\\.bin$",Pattern.CASE_INSENSITIVE);
    public static Map<Integer,String> index(Collection<String> filenames) {
        var result=new HashMap<Integer,String>();var ambiguous=new HashSet<Integer>();
        for(String filename:filenames){
            if(filename.length()>160)continue;
            var match=FILE.matcher(filename.trim());if(!match.matches())continue;
            try {
                int id=Integer.parseInt(match.group(1));String token=match.group(2);
                if(ambiguous.contains(id))continue;
                String prior=result.putIfAbsent(id,token);
                if(prior!=null&&!prior.toLowerCase(Locale.ROOT).equals(token.toLowerCase(Locale.ROOT))){
                    result.remove(id);ambiguous.add(id);
                }
            }catch(NumberFormatException invalid){ /* Out-of-range IDs cannot name an Android effect. */ }
        }
        return Map.copyOf(result);
    }
}
