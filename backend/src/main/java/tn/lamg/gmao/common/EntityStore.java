package tn.lamg.gmao.common;

import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class EntityStore {

    @PersistenceContext
    private EntityManager em;

    public EntityManager manager() {
        return em;
    }

    public String nextNumber(String prefix, String sequence) {
        return (String) em
            .createNativeQuery("select next_business_number(:prefix, cast(:sequence as regclass))")
            .setParameter("prefix", prefix)
            .setParameter("sequence", sequence)
            .getSingleResult();
    }

    public <T extends BaseEntity> T find(Class<T> type, UUID id) {
        var e = em.find(type, id);
        if (e == null || e.archived) throw new ApiException(
            404,
            "NOT_FOUND",
            "Ressource introuvable."
        );
        return e;
    }

    public <T extends BaseEntity> T lock(Class<T> type, UUID id) {
        var e = find(type, id);
        em.lock(e, LockModeType.PESSIMISTIC_WRITE);
        return e;
    }

    public <T extends BaseEntity> List<T> all(Class<T> type) {
        return em
            .createQuery(
                "select e from " +
                    type.getSimpleName() +
                    " e where e.archived=false order by e.createdAt desc",
                type
            )
            .getResultList();
    }

    public <T extends BaseEntity> List<T> matching(Class<T> type, String field, Object value) {
        try {
            type.getField(field);
        } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException("Unknown internal field", ex);
        }
        return em
            .createQuery(
                "select e from " +
                    type.getSimpleName() +
                    " e where e.archived=false and e." +
                    field +
                    "=:value order by e.createdAt",
                type
            )
            .setParameter("value", value)
            .getResultList();
    }

    public <T extends BaseEntity> T save(T e) {
        if (!em.contains(e)) em.persist(e);
        em.flush();
        return e;
    }

    public void expect(BaseEntity e, String match) {
        if (match == null) throw new ApiException(
            428,
            "VERSION_REQUIRED",
            "La version attendue est obligatoire."
        );
        if (!match.equals("\"" + e.version + "\"")) throw new ApiException(
            412,
            "VERSION_CONFLICT",
            "La ressource a changé. Rechargez avant de poursuivre."
        );
    }
}
