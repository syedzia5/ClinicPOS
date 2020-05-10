package com.zcs.clinicpos;

public class UserData {

    private int UDID;
    private String user_id;
    private int roleid;

    public  UserData () {

    }

    public int UDID () {

        return this.UDID;
    }
    public void UDID (int UDID) {

        this.UDID = UDID;
    }

    public String user_id () {

        return this.user_id;
    }
    public void user_id (String user_id) {

        this.user_id = user_id;
    }

    public int roleid () {

        return this.roleid;
    }
    public void roleid (int roleid) {

        this.roleid = roleid;
    }
}
