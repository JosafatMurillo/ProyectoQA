/**
 * Clase del controlador de la entidad User
 * 
 * De La Cruz Díaz Adolfo Ángel; Murillo Hernández Josafat
 * 
 * Versión 1.0
 * 
 * 19/12/2018
 * 
 * Inbo
 */
package mx.inbo.controllers;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import mx.inbo.entities.Quiz;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import mx.inbo.controllers.exceptions.IllegalOrphanException;
import mx.inbo.controllers.exceptions.NonexistentEntityException;
import mx.inbo.datasource.DataBaseInbo;
import mx.inbo.domain.FileHelper;
import mx.inbo.domain.FileSaver;
import mx.inbo.domain.Thumbnail;
import mx.inbo.entities.User;
import mx.inbo.exception.CustomException;

/**
 *
 * @author Josafat
 */
public class UserJpaController implements Serializable {

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public UserJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(User user) {
        if (user.getQuizCollection() == null) {
            user.setQuizCollection(new ArrayList<Quiz>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Quiz> attachedQuizCollection = new ArrayList<Quiz>();
            for (Quiz quizCollectionQuizToAttach : user.getQuizCollection()) {
                quizCollectionQuizToAttach = em.getReference(quizCollectionQuizToAttach.getClass(), quizCollectionQuizToAttach.getIdQuiz());
                attachedQuizCollection.add(quizCollectionQuizToAttach);
            }
            user.setQuizCollection(attachedQuizCollection);
            em.persist(user);
            for (Quiz quizCollectionQuiz : user.getQuizCollection()) {
                User oldIdUserOfQuizCollectionQuiz = quizCollectionQuiz.getIdUser();
                quizCollectionQuiz.setIdUser(user);
                quizCollectionQuiz = em.merge(quizCollectionQuiz);
                if (oldIdUserOfQuizCollectionQuiz != null) {
                    oldIdUserOfQuizCollectionQuiz.getQuizCollection().remove(quizCollectionQuiz);
                    oldIdUserOfQuizCollectionQuiz = em.merge(oldIdUserOfQuizCollectionQuiz);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(User user) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            User persistentUser = em.find(User.class, user.getIdUser());
            Collection<Quiz> quizCollectionOld = persistentUser.getQuizCollection();
            Collection<Quiz> quizCollectionNew = user.getQuizCollection();
            List<String> illegalOrphanMessages = null;
            for (Quiz quizCollectionOldQuiz : quizCollectionOld) {
                if (!quizCollectionNew.contains(quizCollectionOldQuiz)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Quiz " + quizCollectionOldQuiz + " since its idUser field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Collection<Quiz> attachedQuizCollectionNew = new ArrayList<Quiz>();
            for (Quiz quizCollectionNewQuizToAttach : quizCollectionNew) {
                quizCollectionNewQuizToAttach = em.getReference(quizCollectionNewQuizToAttach.getClass(), quizCollectionNewQuizToAttach.getIdQuiz());
                attachedQuizCollectionNew.add(quizCollectionNewQuizToAttach);
            }
            quizCollectionNew = attachedQuizCollectionNew;
            user.setQuizCollection(quizCollectionNew);
            user = em.merge(user);
            for (Quiz quizCollectionNewQuiz : quizCollectionNew) {
                if (!quizCollectionOld.contains(quizCollectionNewQuiz)) {
                    User oldIdUserOfQuizCollectionNewQuiz = quizCollectionNewQuiz.getIdUser();
                    quizCollectionNewQuiz.setIdUser(user);
                    quizCollectionNewQuiz = em.merge(quizCollectionNewQuiz);
                    if (oldIdUserOfQuizCollectionNewQuiz != null && !oldIdUserOfQuizCollectionNewQuiz.equals(user)) {
                        oldIdUserOfQuizCollectionNewQuiz.getQuizCollection().remove(quizCollectionNewQuiz);
                        oldIdUserOfQuizCollectionNewQuiz = em.merge(oldIdUserOfQuizCollectionNewQuiz);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = user.getIdUser();
                if (findUser(id) == null) {
                    throw new NonexistentEntityException("The user with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            User user;
            try {
                user = em.getReference(User.class, id);
                user.getIdUser();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The user with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Quiz> quizCollectionOrphanCheck = user.getQuizCollection();
            for (Quiz quizCollectionOrphanCheckQuiz : quizCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This User (" + user + ") cannot be destroyed since the Quiz " + quizCollectionOrphanCheckQuiz + " in its quizCollection field has a non-nullable idUser field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(user);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<User> findUserEntities() {
        return findUserEntities(true, -1, -1);
    }

    public List<User> findUserEntities(int maxResults, int firstResult) {
        return findUserEntities(false, maxResults, firstResult);
    }

    private List<User> findUserEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(User.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public User findUser(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(User.class, id);
        } finally {
            em.close();
        }
    }

    public int getUserCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<User> rt = cq.from(User.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    /**
     * Funcion validadora que verifica el inicio de sesión
     * @param userName Cadena de caracteres que son el nombre del usuario
     * @param contrasenia Cadena de caracteres que son la contraseña del usuario
     * @throws SQLException Excepcion en caso de no establecer una conexion con la base de datos
     * @throws CustomException Excepcion personalizada para cualquier otra excepcion que no se pueda tratar
     * @throws NoResultException Excepcion enc aso de que no se localizara el usuario
     */
    public void validarLogin(String userName, String contrasenia) throws SQLException,
            CustomException, NoResultException {
        EntityManager em = getEntityManager();
        DataBaseInbo conexion = new DataBaseInbo();

        if (conexion.MySQLConnect() == null) {
            throw new SQLException("Conexión fallida, intentelo más tarde");
        }

        String queryName = "User.findByUsername";
        Query query = em.createNamedQuery(queryName);
        query.setParameter("username", userName);
        User usuario = null;
        try {
            usuario = (User) query.getSingleResult();
        } catch (NoResultException ex) {
            throw new NoResultException("Usuario no encontrado");
        }

        if (!usuario.getContrasenia().equals(contrasenia)) {
            throw new CustomException("Contraseña ingresada incorrecta");
        }
    }

    /**
     * Funcion que se encarga de mandar correos electronicos a los nuevos usuarios de Inbo
     * @param usuario Objeto del tipo User que será el destinatario del correo
     * @throws MessagingException  Excepcion en caso de que no se pueda enviar un correo electronico
     */
    public void correoSignup(User usuario) throws MessagingException {
        try {
            Properties props = new Properties();
            props.setProperty("mail.smtp.host", "smtp.gmail.com");
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.port", "587");
            props.setProperty("mail.smtp.auth", "true");

            Session session = Session.getInstance(props);

            String correoRemitente = "inboconstruccion@gmail.com";
            String passwordRemitente = "InboConstruccion123";
            String correoReceptor = usuario.getEmail();
            String asunto = "Contraseña para Inbo";
            String mensaje = "Hola <b>" + usuario.getUsername() + "</b><br>"
                    + "Esta es tu contraseña para tu cuenta en Inbo <b>" + usuario.getContrasenia() + "</b>";

            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(correoRemitente));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(correoReceptor));
            message.setSubject(asunto);
            message.setText(mensaje, "ISO-8859-1", "html");

            Transport t = session.getTransport("smtp");
            t.connect(correoRemitente, passwordRemitente);
            t.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            t.close();

        } catch (MessagingException ex) {
            throw new MessagingException("Fallo al enviar el correo electronico");
        }

    }

    /**
     * Funcion que crea una contraseña aleatoria basandose en el alfabeto con numeros
     * @param count Limite de caracteres que posee la clave
     * @return Regresa una cadena que será la nueva contraseña
     */
    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    /**
     * Fucnión que agrega a un usuario nuevo a la base de datos
     * @param usuario Objeto de tipo User que será agregado a la base de datos
     * @throws MessagingException Excepción en caso de que no se pueda enviar un correo electronico
     */
    public void agregarUsuario(User usuario) throws MessagingException {

        Thumbnail thumb = usuario.getImage();

        String filePath = FileSaver.createFileName(thumb.getType(), usuario.getIdUser(), usuario.getUsername(), thumb.getExtention());

        FileSaver.saveFile(thumb, filePath);

        usuario.setImagen(filePath);
        usuario.setContrasenia(randomAlphaNumeric(15));
        create(usuario);
        correoSignup(usuario);
    }

    /**
     * Función que modifica a un usuario
     * @param usuario Objeto del tipo User que modifica sus valores por unos nuevos
     * @throws NonexistentEntityException 
     */
    public void editarUsuario(User usuario) throws NonexistentEntityException {
        try {
            Thumbnail thumb = usuario.getImage();

            String filePath = FileSaver.createFileName(thumb.getType(), usuario.getIdUser(), usuario.getUsername(), thumb.getExtention());

            FileSaver.saveFile(thumb, filePath);
            
            usuario.setImagen(filePath);
            edit(usuario);
            System.out.println("El usuario " + usuario.getUsername() + " fue actualizado");
        } catch (NonexistentEntityException ex) {
            throw new NonexistentEntityException("En este momento no es posible cambiar la contraseña");
        } catch (Exception ex) {
            Logger.getLogger(UserJpaController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Funcion que busca un usuario basado en su nombre de usuario
     * @param username Cadena de caracteres que sirve como indice de busqueda
     * @return Regresa un objeto del tipo User que es el usuario encontrado.
     */
    public User obtenerUsuario(String username) {
        EntityManager em = getEntityManager();
        String queryName = "User.findByUsername";
        Query query = em.createNamedQuery(queryName);
        query.setParameter("username", username);
        User usuario = null;
        try {
            usuario = (User) query.getSingleResult();
            Thumbnail thumb = getThumb(usuario.getImagen());
            usuario.setImage(thumb);
        } catch (NoResultException ex) {
            throw new NoResultException("Usuario no encontrado");
        }

        return usuario;
    }

    /**
     * 
     * @param fileName
     * @return 
     */
    public Thumbnail getThumb(String fileName) {
        Thumbnail thumb = new Thumbnail();
        thumb.setType("User");

        File imageFile = new File(System.getProperty("user.home") + "/InboRepo/" + fileName);

        int extIndex = fileName.lastIndexOf(".");
        String imageExtention = fileName.substring(extIndex + 1).toLowerCase();
        thumb.setExtention(imageExtention);

        byte[] image = FileHelper.parseFileToBytes(imageFile, imageExtention);
        thumb.setImage(image);

        return thumb;
    }

}
