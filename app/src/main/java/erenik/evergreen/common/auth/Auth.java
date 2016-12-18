package erenik.evergreen.common.auth;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import java.security.*;

/**
 * Created by Emil on 2016-11-23.
 */
public class Auth {
    
/*
    public static PrivateKey GetPrivateKey(String filename) throws Exception 
    {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        PKCS8EncodedKeySpec spec =
          new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static PublicKey GetPublicKey(String filename) throws Exception 
    {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        X509EncodedKeySpec spec =
          new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
  */
    /// Generates 2 keys. Saves them into files and returns the file-names.
    public static String[] GenerateKeys() throws NoSuchAlgorithmException, NoSuchProviderException
    {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();
        
        /// TODO
        return null;
    }
    

    static String Sign(String dataToSign, String signature) throws NoSuchAlgorithmException, SignatureException, Exception
    { 
        Signature signer = Signature.getInstance("SHA256withRSA");
        PrivateKey privateKey = null;
    //    privateKey = GetPrivateKey("evergreen-private.ppk");
        signer.initSign(privateKey); // PKCS#8 is preferred
        signer.update(dataToSign.getBytes());
        byte[] output = signer.sign();
        return new String(output);
    }    
    
    
    private static final byte[] salt = {
            ( byte )0xf5, ( byte )0x33, ( byte )0x01, ( byte )0x2a,
            ( byte )0xb2, ( byte )0xcc, ( byte )0xe4, ( byte )0x7f
    };
        
    static SecretKey GenSecretKey(String password) throws InvalidKeySpecException, NoSuchAlgorithmException
    {
        PBEKeySpec keySpec = new PBEKeySpec( password.toCharArray() );   // obtain instance for secret key factory
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );  // generate secret key for encryption
        return keyFactory.generateSecret(keySpec);
    }
    
    /// Mode should be Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
    static Cipher InitCipher(String password, int mode) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        SecretKey secretKey = GenSecretKey(password);
        int iterationCount = 100;// specifies parameters used with password based encryption
        PBEParameterSpec parameterSpec = new PBEParameterSpec( salt, iterationCount );   // obtain cipher instance reference
        Cipher cipher = null;
        cipher = Cipher.getInstance( "PBEWithMD5AndDES" );   // initialize cipher in encrypt mode
        cipher.init( mode, secretKey, parameterSpec );
        return cipher;
    }

    public static String Encrypt(String textToEncrypt, String password)
    {
        // create secret key and get cipher instance
        try {
            Cipher cipher = InitCipher(password, Cipher.ENCRYPT_MODE);
            byte[] bytesToEncrypt = textToEncrypt.getBytes();
            byte[] cipherText = new byte[cipher.getOutputSize(bytesToEncrypt.length)];
            int ctLength = cipher.update(bytesToEncrypt, 0, bytesToEncrypt.length, cipherText, 0);
            ctLength += cipher.doFinal(cipherText, ctLength);
            String encrypted = new String(cipherText);
            return encrypted;
        } catch ( Exception exception ) {
            exception.printStackTrace();
            System.exit( 1 );
        }
        return null;
    }
    public static String Decrypt(String textToDecrypt, String password)
    {
        // create secret key and get cipher instance
        try {
            Cipher cipher = InitCipher(password, Cipher.DECRYPT_MODE);
            byte[] bytesToDecrypt = textToDecrypt.getBytes();
            int decryptedLength = cipher.getOutputSize(bytesToDecrypt.length);
            System.out.println("decryptedLength: "+decryptedLength);
            byte[] cipherText = new byte[decryptedLength];
            // decryption pass
            byte[] plainText = new byte[cipher.getOutputSize(decryptedLength)];
            int ptLength = cipher.update(cipherText, 0, decryptedLength, plainText, 0);
            System.out.println(new String(plainText));

            int eights = ptLength /8;
            int newPtLength = (eights + 1) * 8; 

            ptLength += cipher.doFinal(plainText, ptLength);
            System.out.println(new String(plainText));
            System.out.println(ptLength);
        } catch ( Exception exception ) {
            exception.printStackTrace();
            System.exit( 1 );
        }
        return null;
    }

    public static void UnitTest() throws Exception 
    {
        String text = "Hello world!";
        String s3 = Auth.Sign(text, "yeah");
        System.out.println("Text: "+text+" \nSigned: "+s3);
        
        
        String s2 = Auth.Encrypt(text, "albatross");
        System.out.println(text+" encrypted: "+s2+" ");
        String de = Auth.Decrypt(s2, "albatross");
        System.out.println(s2+" decrypted: "+de+" ");
    }
}


/*
// salt for password-based encryption-decryption algorithm

	// iteration count
	private int iterationCount = 100;

	//user input components.
	private JTextField passwordTextField;
	private JTextField fileNameTextField;
	private JEditorPane fileContentsEditorPane;

	// frame constructor
	public EncipherDecipher()
	{
		// set security provider
		Security.addProvider( new SunJCE() );

		// initialize main frame
		setSize( new Dimension( 400, 400 ) );
		setTitle( "Encryption and Decryption Example" );

		// construct top panel
		JPanel topPanel = new JPanel();
		topPanel.setBorder( BorderFactory.createLineBorder(
				Color.black ) );
		topPanel.setLayout( new BorderLayout() );

		// panel where password and file name labels will be placed
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout( new GridLayout( 2, 1 ) );
		JLabel passwordLabel = new JLabel( " Password: " );
		JLabel fileNameLabel = new JLabel( " File Name: " );
		labelsPanel.add( fileNameLabel );
		labelsPanel.add( passwordLabel );
		topPanel.add( labelsPanel, BorderLayout.WEST );

		//panel where password and file name textfields placed
		JPanel textFieldsPanel = new JPanel();
		textFieldsPanel.setLayout( new GridLayout( 2, 1 ) );
		passwordTextField = new JPasswordField();

		fileNameTextField = new JTextField();
		textFieldsPanel.add( fileNameTextField );
		textFieldsPanel.add( passwordTextField );
		topPanel.add( textFieldsPanel, BorderLayout.CENTER );

		// construct middle panel
		JPanel middlePanel = new JPanel();
		middlePanel.setLayout( new BorderLayout() );

		// construct and place title label for contents pane
		JLabel fileContentsLabel = new JLabel();
		fileContentsLabel.setText( " File Contents" );
		middlePanel.add( fileContentsLabel, BorderLayout.NORTH );

		// initialize and place editor pane within scroll panel
		fileContentsEditorPane = new JEditorPane();
		middlePanel.add(
				new JScrollPane( fileContentsEditorPane ), BorderLayout.CENTER );

		// construct bottom panel
		JPanel bottomPanel = new JPanel();

		// create encrypt button
		JButton encryptButton;
		encryptButton= 	new JButton( "Encrypt and Write to File" );

		encryptButton.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					encryptAndWriteToFile();
				}
			}
		);
		bottomPanel.add(encryptButton);

		// create decrypt button
		JButton decryptButton =
			new JButton( "Read from File and Decrypt" );

		decryptButton.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event )
				{
					readFromFileAndDecrypt();
				}
			}
		);
		bottomPanel.add( decryptButton );

		// initialize main frame window
		JPanel contentPane = ( JPanel ) this.getContentPane();
		contentPane.setLayout( new BorderLayout() );
		contentPane.add( topPanel, BorderLayout.NORTH );
		contentPane.add( middlePanel, BorderLayout.CENTER );
		contentPane.add( bottomPanel, BorderLayout.SOUTH );
	} // end constructor

	//obtain contents from editor pane and encrypt
	private void encryptAndWriteToFile()
	{
		// obtain user input
		String originalText = fileContentsEditorPane.getText();
		String password = passwordTextField.getText();
		String fileName = fileNameTextField.getText();

		// create secret key and get cipher instance
		Cipher cipher = null;

		try {
			// create password based encryption key object
			PBEKeySpec keySpec = new PBEKeySpec( password.toCharArray() );

			// obtain instance for secret key factory
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );

			// generate secret key for encryption
			SecretKey secretKey = keyFactory.generateSecret( keySpec );

			// specifies parameters used with password based encryption
			PBEParameterSpec parameterSpec = new PBEParameterSpec( salt, iterationCount );

			// obtain cipher instance reference
			cipher = Cipher.getInstance( "PBEWithMD5AndDES" );

			// initialize cipher in encrypt mode
			cipher.init( Cipher.ENCRYPT_MODE, secretKey, parameterSpec );
		}
		catch ( Exception exception ) {
			exception.printStackTrace();
			System.exit( 1 );
		}

		//create array of bytes
		byte[] outputArray = null;
		try {
			outputArray = originalText.getBytes( "ISO-8859-1" );
		}
		catch ( UnsupportedEncodingException exception ) {
			exception.printStackTrace();
			System.exit( 1 );
		}

		// create FileOutputStream
		File file = new File( fileName );
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream( file );
		}
		catch ( IOException exception ) {
			exception.printStackTrace();
			System.exit( 1 );
		}

		// create CipherOutputStream
		CipherOutputStream out = new CipherOutputStream( fileOutputStream, cipher );

		// write contents to file and close
		try {
			out.write( outputArray );
			out.flush();
			out.close();
		}
		catch ( IOException exception ) { 	//Handle IOException
			exception.printStackTrace();
			System.exit( 1 );
		}

		// contain bytes read from file
		Vector fileBytes = new Vector();
		// read contents from file to show user encrypted text
		try {
			FileInputStream in = new FileInputStream( file );
			// read bytes from stream.
			byte contents;
			while ( in.available() > 0 )
			{
				contents = ( byte )in.read();
				fileBytes.add( new Byte( contents ) );
			}
			in.close();
		}
		catch ( IOException exception ) {
			exception.printStackTrace();
			System.exit( 1 );
		}

		// create byte array from contents in Vector fileBytes
		byte[] encryptedText = new byte[ fileBytes.size() ];
		for ( int i = 0; i < fileBytes.size(); i++ )
		{
			encryptedText[ i ] = ( ( Byte ) fileBytes.elementAt( i ) ).byteValue();
		}

		// update Editor Pane contents
		fileContentsEditorPane.setText( new String( encryptedText ) );
	}

	// obtain contents from file and decrypt
	private void readFromFileAndDecrypt()
	{
		// used to rebuild byte list
		Vector fileBytes = new Vector();

		// obtain user input
		String password = passwordTextField.getText();
		String fileName = fileNameTextField.getText();

		// create secret key
		Cipher cipher = null;
		try {
			// create password based encryption key object
			PBEKeySpec keySpec =
				new PBEKeySpec( password.toCharArray() );

			// obtain instance for secret key factory
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );

			//generate secret key for encryption
			SecretKey secretKey = keyFactory.generateSecret( keySpec );

			// specifies parameters used with password based encryption
			PBEParameterSpec parameterSpec = new PBEParameterSpec( salt, iterationCount );

			// obtain cipher instance reference.
			cipher = Cipher.getInstance( "PBEWithMD5AndDES" );

			// initialize cipher in decrypt mode
			cipher.init( Cipher.DECRYPT_MODE, secretKey, parameterSpec );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		// read and decrypt contents from file
		try {
			File file = new File( fileName );
			FileInputStream fileInputStream =
				new FileInputStream( file );
			CipherInputStream in =
				new CipherInputStream( fileInputStream, cipher );

			// read bytes from stream.
			byte contents = ( byte ) in.read();
			while ( contents != -1 )
			{
				fileBytes.add( new Byte( contents ) );
				contents = ( byte ) in.read();
			}
			in.close();
		}
		catch ( IOException exception ) {
			exception.printStackTrace();
			System.exit( 1 );
		}

		// create byte array from contents in Vector fileBytes
		byte[] decryptedText = new byte[ fileBytes.size() ];
		for ( int i = 0; i < fileBytes.size(); i++ ) {
			decryptedText[ i ] =
				( ( Byte )fileBytes.elementAt( i ) ).byteValue();
		}
		// update Editor Pane contents.
		fileContentsEditorPane.setText( new String( decryptedText ) );
	}


	// create frame and display
	public static void main( String[] args )
	{
		EncipherDecipher crypto = new EncipherDecipher();
		crypto.validate(); crypto.setVisible( true );
	}
}
 */