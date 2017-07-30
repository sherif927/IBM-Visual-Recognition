package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.AddImageToCollectionOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.CollectionImage;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.FindSimilarImagesOptions;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MainController {
	private VisualRecognition service=null;
	private String username="";
	private String filePath="C:/Users/Kagune/Pics/";
	private String collectionID="Employees_95d26d";
	
	//Just a label to notify the user of any errors or when Login/Registration is complets
	@FXML
	private Label mLabel;
	
	//The imageView in which the the pictures will be displayed
	@FXML
	private ImageView mImageView;
	
	//TextField used for user input in registration mode
	@FXML
	private TextField mTextField;

	//Constructor for the Controller-Class
	public MainController(){
		service=new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
		service.setApiKey("6aa53dc8ba1dc7b6a58fc4e9316a1972e0fd49ed");
		service.setEndPoint("https://gateway-a.watsonplatform.net/visual-recognition/api");
	}
	
	
	// <summary>
	// Function that's triggered when the user hits the register button
	// </summary>
	public void register(){
		username=mTextField.getText();
		mLabel.setText(username);
		registerUser();
	}
	
	// <summary>
	// Function that's triggered when the user hits the login button
	// </summary>
	public void login(){
		userLogin();
	}
	
	// <summary>
	// loads an image into the imageview from it's location
	// </summary>
	// <param name="filePath">the location of the image to be loaded into the imageView</param>
	public void loadImage(String filePath){
	
		FileInputStream is;
		try {
			is = new FileInputStream(filePath);
			mImageView.setImage(new Image(is));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	// <summary>
	// Moves the recently captured photo from the captured folder to it's user's own folder
	// by deriving the name of the user from the image name
	// </summary>
	// <param name="imageName">the name of the image</param>
	public void rearrange(String imageName){
		String[]props=imageName.split("-");
		File user=new File(filePath+props[0]),captured=new File(filePath+"Captured"),current;
		File[]currentFiles=captured.listFiles();
		current=currentFiles[0];
		File[]files=user.listFiles();
		String dest=filePath+props[0]+"/"+props[0]+"-img-"+files.length+".png";
		System.out.println(dest);
		current.renameTo(new File(dest));
		loadImage(files[0].getPath());
	}
	
	// <summary>
	// Checks whether or not there is a webcam , if there is a webcam present
	// then the cam captures a photo and creates a new folder with the username typed in the textfield
	// </summary>
	// <param name="userName">the username typed in the textfield</param>
	public  void capturePhoto(String userName){
		Webcam mWebcam=Webcam.getDefault();
		mLabel.setText("Please Look into the camera..");
		File f=new File(filePath+userName);
		File[]files=f.listFiles();
		int size=(files==null)?0:files.length;
		if(mWebcam!=null){
			System.out.println(mWebcam.getName()+" is online");
			System.out.println("Photo is being taken..");
			mWebcam.open();
			try {
				File pic=new File(f.getPath()+"/"+userName+"-img-"+size+".png");
				ImageIO.write(mWebcam.getImage(), "PNG", pic);
				System.out.println("All done !");
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				mWebcam.close();
			}
		}else {
			System.out.println("Camera not working properly..");
		}
	}
	
	// <summary>
	// The function responsible for creating the directory named after the user
	// it checks whether a directory with the same name exists , if so it prompts the user to choose another name
	// </summary>
	public  void registerUser(){
			File f = new File(filePath+username);
			File img=null;
			if(!f.exists() && !f.isDirectory()) { 
			    if(f.mkdir()){
			    	capturePhoto(username);
			    	addPhoto(username);
			    	mLabel.setText("Welcome "+username);
			    	img=getFirstFile(new File(filePath+username));
			    	loadImage(img.getPath());
			    }
			}else{
				System.out.println("username taken , try another one");
			}
		
	}
	// <summary>
	// Captures a photo , and places it in the captured folder for further processing
	// it then finds the most similar image from the collection and then re-arranges.
	// </summary>
	public void userLogin(){
	capturePhoto("Captured");
	File captured;
	File[]captures=new File(filePath+"Captured").listFiles();
	captured=captures[0];
	mLabel.setText("Welcome user!");
	String max=findSimilarFaces(captured);
	System.out.println(max);
	rearrange(max);
	
	}
	
	// <summary>
	// Adds a photo to the collection
	// </summary>
	// <param name="username">the user whose photo should be uploaded</param>

	public  void addPhoto(String username){
		File f=new File(filePath+username);
		File img=getFirstFile(f);
		add(img);
	}
	
	// <summary>
	// retrieves the first file present in a directory
	// </summary>
	// <param name="directory">the handle of the directory</param>
	// <returns>A file object</returns>
	public File getFirstFile(File directory){
		File[] files=directory.listFiles();
		return files[0];
	}
	
	// <summary>
	// Finds the most similar image to the provided image
	// </summary>
	// <param name="img">The image</param>
	// <returns>String holding the name of the file with the highest similarity</returns>
	public  String findSimilarFaces(File img){
		FindSimilarImagesOptions options = new FindSimilarImagesOptions.Builder()
			    .image(img)
			    .collectionId(collectionID)
			    .build();
			List<CollectionImage> result = service.findSimilarImages(options).execute();
			double score=0;
			String maxImg="";
			for(CollectionImage i:result){
				if(i.getScore()>score){
					maxImg=i.getImageFile();
					score=i.getScore();
				}
			}
			return maxImg;
	}
	
	// <summary>
	// The method that actually handles the image upload to the collection
	// </summary>
	// <param name="img">Image to be uploaded</param>
	public  void add(File img){
		AddImageToCollectionOptions.Builder options=new AddImageToCollectionOptions.Builder().images(img);
		options.collectionId(collectionID);
		AddImageToCollectionOptions imgOptions=options.build();
		service.addImageToCollection(imgOptions).execute();
		
	}
}
