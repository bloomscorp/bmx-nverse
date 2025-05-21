package com.bloomscorp.nverse.dao;

import com.bloomscorp.nverse.pojo.NVerseRole;

public interface NVerseUserRoleDAO<
    E extends Enum<E>,
    R extends NVerseRole<E>
> {
    int addNewRole(R role);
}
