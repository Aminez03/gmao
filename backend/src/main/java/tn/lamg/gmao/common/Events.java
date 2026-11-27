package tn.lamg.gmao.common;

import java.util.*;
import org.springframework.stereotype.Service;
import tn.lamg.gmao.modules.pilotage.model.*;
import tn.lamg.gmao.security.*;

@Service
public class Events {

    final EntityStore store;
    final CurrentUser current;
    final UserRepository users;

    public Events(EntityStore s, CurrentUser c, UserRepository u) {
        store = s;
        current = c;
        users = u;
    }

    public void audit(String route, BaseEntity entity, String action) {
        var a = new AuditEvent();
        a.actorId = current.get().id;
        a.action = action;
        a.resourceType = route;
        a.resourceId = entity.id;
        a.details = "Version " + entity.version;
        store.save(a);
    }

    public void audit(String route, BaseEntity entity, String action, String details) {
        var a = new AuditEvent();
        a.actorId = current.get().id;
        a.action = action;
        a.resourceType = route;
        a.resourceId = entity.id;
        a.details = "Version " + entity.version + " ; " + details;
        store.save(a);
    }

    public void account(UserAccount user, String action) {
        var a = new AuditEvent();
        a.actorId = current.get().id;
        a.action = action;
        a.resourceType = "users";
        a.resourceId = user.id;
        a.details = "Rôle " + user.role + " ; actif=" + user.active + " ; version=" + user.version;
        store.save(a);
    }

    public void notify(UUID recipient, String title, String type, UUID id) {
        if (recipient == null) return;
        if (
            store
                .all(Notification.class)
                .stream()
                .anyMatch(
                    n ->
                        recipient.equals(n.recipientId) &&
                        title.equals(n.title) &&
                        java.util.Objects.equals(id, n.resourceId)
                )
        ) return;
        var n = new Notification();
        n.recipientId = recipient;
        n.title = title;
        n.resourceType = type;
        n.resourceId = id;
        store.save(n);
    }

    public void supervisors(String title, String type, UUID id) {
        users
            .findAll()
            .stream()
            .filter(u -> u.active && Set.of("ADMIN", "RESPONSABLE_TECHNIQUE").contains(u.role))
            .forEach(u -> notify(u.id, title, type, id));
    }
}
