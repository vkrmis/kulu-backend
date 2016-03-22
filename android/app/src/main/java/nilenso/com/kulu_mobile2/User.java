package nilenso.com.kulu_mobile2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.realm.RealmObject;
import io.realm.annotations.Index;

public class User extends RealmObject {
    private String displayName;

    @Index
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setCurrentUserInfo(String email) {
        setEmail(email);
    }

}
