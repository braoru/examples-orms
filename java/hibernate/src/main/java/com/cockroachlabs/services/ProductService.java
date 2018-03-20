package com.cockroachlabs.services;

import com.cockroachlabs.model.Product;
import com.cockroachlabs.util.SessionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.ws.rs.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

@Path("/product")
public class ProductService {

    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Produces("application/json")
    public String getProducts() {
        try (Session session = SessionUtil.getSession()) {
            Query query = session.createQuery("from Product");
            List products = query.list();
            return mapper.writeValueAsString(products);
        } catch (JsonProcessingException e) {
            return e.toString();
        }
    }

    @POST
    @Produces("application/json")
    public String createProduct(String body) {
        try (Session session = SessionUtil.getSession()) {
            Product newProduct = mapper.readValue(body, Product.class);
            session.save(newProduct);

            return mapper.writeValueAsString(newProduct);
        } catch (IOException e) {
            return e.toString();
        }
    }

    @GET
    @Path("/{productID}")
    @Produces("application/json")
    public String getProduct(@PathParam("productID") long productID) {
        try (Session session = SessionUtil.getSession()) {
            Product product = session.get(Product.class, productID);
            if (product == null) {
                throw new NotFoundException();
            }

            return mapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            return e.toString();
        }
    }

    @PUT
    @Path("/{productID}")
    @Produces("application/json")
    public String updateProduct(@PathParam("productID") long productID, String body) {
        try (Session session = SessionUtil.getSession()) {
            Product updateInfo = mapper.readValue(body, Product.class);
            updateInfo.setId(productID);

            Product updatedProduct = (Product) session.merge(updateInfo);
            return mapper.writeValueAsString(updatedProduct);
        } catch (IOException e) {
            return e.toString();
        }
    }

    @DELETE
    @Path("/{productID}")
    @Produces("text/plain")
    public String deleteProduct(@PathParam("productID") long productID) {
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();

            Query deleteReferencing = session.createQuery("delete from Order where product_id = :id");
            deleteReferencing.setParameter("id", productID);
            deleteReferencing.executeUpdate();

            Query deleteProduct = session.createQuery("delete from Product where id = :id");
            deleteProduct.setParameter("id", productID);

            int rowCount = deleteProduct.executeUpdate();
            if (rowCount == 0) {
                tx.rollback();
                throw new NotFoundException();
            }
            tx.commit();
            return "ok";
        }
    }

    @GET
    @Path("/inflation")
    @Produces("text/plain")
    public String increasePriceByOne(){
        Logger.getLogger("com.cockroachlabs").info("increasePriceByOne");
        try (Session session = SessionUtil.getSession()) {

            Transaction tx = session.beginTransaction();

            Query savepointQuery = session.createNativeQuery("SAVEPOINT COCKROACH_RESTART;");
            savepointQuery.executeUpdate();

            while(true){
                try {
                    Query query = session.createQuery("from Product");
                    List<Product> products = query.list();


                    for (Product product : products) {
                        Product p = session.get(Product.class, product.getId());
                        Logger.getLogger("com.cockroachlabs").info("id:" + p.getId());
                        p.setPrice(p.getPrice().add(BigDecimal.ONE));
                        session.merge(p);
                    }

                    tx.commit();
                    return "ok";
                }catch(Exception e){
//                    if(e.getErrorCode() == 40001){
                    e.printStackTrace();

                    try {
                        Query rollbackSavepointQuery = session.createNativeQuery("ROLLBACK TO SAVEPOINT COCKROACH_RESTART;");
                        rollbackSavepointQuery.executeUpdate();
                        Logger.getLogger("com.cockroachlabs").info("SUCCESSFULLY ROLLBACK TO SAVEPOINT");
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
//                    }

                    //e.printStackTrace();
                }
            }

        }catch(Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }





    @GET
    @Path("/inflation/{productID}")
    @Produces("text/plain")
    public String increasePriceByOne2(@PathParam("productID") long productID){
        Logger.getLogger("com.cockroachlabs").info("increasePriceByOne2");
        try (Session session = SessionUtil.getSession()) {

            Transaction tx = session.beginTransaction();

            Query savepointQuery = session.createNativeQuery("SAVEPOINT COCKROACH_RESTART;");
            savepointQuery.executeUpdate();

            try {
                Query query = session.createNativeQuery("SELECT price FROM Products WHERE id= " + productID + ";");
                List p2 = query.getResultList();

                Query q2 = session.createNativeQuery("UPDATE Products SET price = price + 1 where id=" + productID + ";");
                q2.executeUpdate();

                tx.commit();
                return "ok";
            }catch(Exception e){
                e.printStackTrace();
                Query rollbackSavepointQuery = session.createNativeQuery("ROLLBACK TO SAVEPOINT COCKROACH_RESTART;");
                rollbackSavepointQuery.executeUpdate();
                Logger.getLogger("com.cockroachlabs").info("SUCCESSFULLY ROLLBACK TO SAVEPOINT");
                return "roll";
            }

        }catch(Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }

}
