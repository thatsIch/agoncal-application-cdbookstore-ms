package org.agoncal.application.cdbookstore.view.shopping;

import org.agoncal.application.cdbookstore.model.Item;
import org.agoncal.application.cdbookstore.util.Auditable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

@Named
@RequestScoped
@Transactional
public class RatedItemsBean {

    // ======================================
    // =          Injection Points          =
    // ======================================

    List<Item> topRatedCDs;
    List<Item> topRatedBooks;
    Set<Item> randomItems = new HashSet<>();
    @Inject
    private FacesContext facesContext;

    // ======================================
    // =             Attributes             =
    // ======================================
    @Inject
    private Logger logger;
    @Inject
    private EntityManager em;

    // ======================================
    // =         Lifecycle methods          =
    // ======================================

    @PostConstruct
    private void init() {
        doFindTopRatedBooks();
        doFindTopRatedCDs();
        doFindRandomThree();

    }

    // ======================================
    // =          Business methods          =
    // ======================================

    @Auditable
    private void doFindRandomThree() {
        int min = em.createQuery("select min (i.id) from Item i", Long.class).getSingleResult().intValue();
        int max = em.createQuery("select max (i.id) from Item i", Long.class).getSingleResult().intValue();

        while (randomItems.size() < 3) {
            long id = new Random().nextInt((max - min) + 1) + min;
            Item item = em.find(Item.class, id);
            if (item != null)
                randomItems.add(item);
        }
    }

    @Auditable
    private void doFindTopRatedCDs() {

        Response response;

        // Tries on port 8080 if not 8081
        try {
            response = ClientBuilder.newClient().target("http://localhost:8080/msTopCDs").request(MediaType.APPLICATION_JSON).get();
        } catch (Exception e) {
            response = ClientBuilder.newClient().target("http://localhost:8081/msTopCDs").request(MediaType.APPLICATION_JSON).get();
        }

        if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            try {
                response = ClientBuilder.newClient().target("http://localhost:8081/msTopCDs").request(MediaType.APPLICATION_JSON).get();
            } catch (Exception e) {
                // swallow exception
            }
        }

        if (response != null && response.getStatus() != Response.Status.OK.getStatusCode()) return;

        topRatedCDs = getTopRatedItems(response);
    }

    @Auditable
    private void doFindTopRatedBooks() {

        Response response;

        // Tries on port 8080 if not 8082
        try {
            response = ClientBuilder.newClient().target("http://localhost:8080/msTopBooks").request(MediaType.APPLICATION_JSON).get();
        } catch (Exception e) {
            response = ClientBuilder.newClient().target("http://localhost:8082/msTopBooks").request(MediaType.APPLICATION_JSON).get();
        }

        if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            try {
                response = ClientBuilder.newClient().target("http://localhost:8082/msTopBooks").request(MediaType.APPLICATION_JSON).get();
            } catch (Exception e) {
                // swallow exception
            }
        }

        if (response != null && response.getStatus() != Response.Status.OK.getStatusCode()) return;

        topRatedBooks = getTopRatedItems(response);
    }

    private List<Item> getTopRatedItems(Response response) {

        List<Item> topRatedItems = null;
        String body = response.readEntity(String.class);

        List<Long> topRatedCDIds = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonArray array = reader.readArray();
            for (int i = 0; i < array.size(); i++) {
                topRatedCDIds.add((long) array.getJsonObject(i).getInt("id"));
            }
        }

        if (!topRatedCDIds.isEmpty()) {
            logger.info("Top rated books ids " + topRatedCDIds);
            TypedQuery<Item> query = em.createNamedQuery(Item.FIND_TOP_RATED, Item.class);
            query.setParameter("ids", topRatedCDIds);
            topRatedItems = query.getResultList();
            logger.info("Number of top rated items found " + topRatedItems.size());
        }

        return topRatedItems;
    }

    // ======================================
    // =        Getters and Setters         =
    // ======================================

    public List<Item> getTopRatedCDs() {
        return topRatedCDs;
    }

    public void setTopRatedCDs(List<Item> topRatedCDs) {
        this.topRatedCDs = topRatedCDs;
    }

    public List<Item> getTopRatedBooks() {
        return topRatedBooks;
    }

    public void setTopRatedBooks(List<Item> topRatedBooks) {
        this.topRatedBooks = topRatedBooks;
    }

    public List<Item> getRandomItems() {
        return new ArrayList<>(randomItems);
    }
}
