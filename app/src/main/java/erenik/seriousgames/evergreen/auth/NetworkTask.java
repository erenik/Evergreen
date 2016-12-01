package erenik.seriousgames.evergreen.auth;

import android.os.AsyncTask;

import java.util.AbstractCollection;
import java.util.ArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.WebTarget;

import erenik.seriousgames.evergreen.R;

/**
 * Created by Emil on 2016-11-23.
 */
public class NetworkTask extends AsyncTask<Void, Void, Boolean>
{
    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private final String mEmail;
    private final String mPassword;
    private ArrayList<NetworkTaskListener> listeners = new ArrayList<>();
    private WebTarget target;
    private Client client;
    public NetworkTask(String email, String password) {
        mEmail = email;
        mPassword = password;
    }

    /// Attempt to register using the given credentials.
    void Register()
    {

    }
    public static final String serverURI = "http://www.erenik.com:8080/evergreen/";
    void SetTargetServer()
    {
        client = ClientBuilder.newClient();
        target = client.target(serverURI);
    }
    /// Save character data to server.
    void SaveData()
    {
        // Send it.
        String playerJsonForm = "{\"name\":\"Erenik\"}";
        Response r = target.path("players").request().post(Entity.json(playerJsonForm));
    }

    /// Load character data from server.
    void LoadData()
    {

    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        // TODO: attempt authentication against a network service.
        try {
            // Simulate network access.
            Thread.sleep(2000);

            /// Open port.

            /// Send data.


        } catch (InterruptedException e) {
            return false;
        }

        final String[] DUMMY_CREDENTIALS = new String[]{
                "foo@example.com:hello", "bar@example.com:world"
        };
        for (String credential : DUMMY_CREDENTIALS) {
            String[] pieces = credential.split(":");
            if (pieces[0].equals(mEmail)) {
                // Account exists, return true if the password matches.
                return pieces[1].equals(mPassword);
            }
        }

        // TODO: register the new account here.
        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success)
    {
        for (int i = 0; i < listeners.size(); ++i)
            listeners.get(i).OnTaskCompleted(success);
    }

    @Override
    protected void onCancelled()
    {
        for (int i = 0; i < listeners.size(); ++i)
            listeners.get(i).OnTaskCanceled();
    }

    public void AddListener(NetworkTaskListener listener) {
        listeners.add(listener);
    }
}
