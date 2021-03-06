package controller;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import application.Main;
import beans.Customer;
import beans.Room;
import beans.User;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import model.objects.CustomerModel;
import model.objects.RoomModel;
import tools.Tools;

public class HomeController implements Initializable {
	public Utility utility;
	private static CustomerModel customerModel;
	private static RoomModel roomModel;
	
	//***** HEADER ****
	@FXML
	private Label userlbl;
	@FXML
	private MenuButton menuBtn_currentUser;
	
	//***** TAB - CUSTOMER *****
	@FXML
	TextField searchCustomer;
	@FXML
	Button btnAddCustomer;
	@FXML
	TableView<Customer> customerTable;	
	@FXML
	TableColumn<Customer, String> cIdCard;	
	@FXML
	TableColumn<Customer, String> cDocumentType;
	@FXML
	TableColumn<Customer, String> cFirstname;
	@FXML
	TableColumn<Customer, String> cLastname;
	@FXML
	TableColumn<Customer, String> cEmail;
	@FXML
	TableColumn<Customer, String> cPhone;
	@FXML
	TableColumn<Customer, String> cBirthdate;
	
	//***** TAB - ROOM *****
	@FXML
	TextField searchRoom;
	@FXML
	Button btnAddRoom;
	@FXML
	TableView<Room> roomTable;
	@FXML
	TableColumn<Room, String> cRoomNumber;	
	@FXML
	TableColumn<Room, String> cPrice;
	@FXML
	TableColumn<Room, String> cStatus;
	@FXML
	TableColumn<Room, String> cFloor;
	@FXML
	TableColumn<Room, String> cSize;
	@FXML
	TableColumn<Room, String> cBeds;
	@FXML
	TableColumn<Room, String> cTv;
	@FXML
	TableColumn<Room, String> cFan;	

	private static ObservableList<Room> roomTableData = FXCollections.observableArrayList();	
	private static ObservableList<Customer> customerTableData = FXCollections.observableArrayList();

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		utility = new Utility();
		customerModel = new CustomerModel();
		roomModel = new RoomModel();
		
		User current_user = (User) Main.sessionData.get("current_user");
		String firstName = current_user.getFirstname() != null ? current_user.getFirstname() : "";
		String lastName = current_user.getLastname() != null ? current_user.getLastname().toUpperCase() : "";
		if (current_user != null) {
			userlbl.setText("Bienvenue " + firstName + " " + lastName);
			menuBtn_currentUser.setText(firstName + " " +lastName);
		}
		
		//***** CUSTOMER *****
		cIdCard.setCellValueFactory(new PropertyValueFactory<Customer, String>("id_card"));
		cDocumentType.setCellValueFactory(new PropertyValueFactory<Customer, String>("documentType"));
		cFirstname.setCellValueFactory(new PropertyValueFactory<Customer, String>("firstname"));
		cLastname.setCellValueFactory(new PropertyValueFactory<Customer, String>("lastname"));
		cEmail.setCellValueFactory(new PropertyValueFactory<Customer, String>("email"));
		cPhone.setCellValueFactory(new PropertyValueFactory<Customer, String>("phone"));
		cBirthdate.setCellValueFactory(new PropertyValueFactory<Customer, String>("birthdate"));	
		
		refreshDataCustomer(); 
		customerTable.setItems(customerTableData);
		customerTable.setRowFactory(new Callback<TableView<Customer>, TableRow<Customer>>() {
			@Override
			public TableRow<Customer> call(TableView<Customer> tableView) {
				final TableRow<Customer> row = new TableRow<>();
				final ContextMenu contextMenu = new ContextMenu();
				contextMenu.setId("contextMenu");
				final MenuItem deleteMenuItem = new MenuItem("Supprimer");
				final MenuItem editMenuItem = new MenuItem("Modifier");

				// Set context menu on row, but use a binding to make it only show for non-empty rows:
				row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(contextMenu));
				
				contextMenu.getItems().addAll(editMenuItem, deleteMenuItem);

				deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						Customer customer = customerTable.getItems().get(row.getIndex()).getCustomer();
						Alert alert = Tools.showConfirmationDialog("Voulez-vous vraiment supprimer ce client : " + customer.getFirstname() + " " + customer.getLastname() +" ?");
						
						Optional<ButtonType> result = alert.showAndWait();
						if (result.get() == ButtonType.CANCEL) {
							alert.hide();
						} 
						else {				
							customerModel.deleteCustomer(customer.getId_customer());
							refreshDataCustomer();
						}
					}
				});

				editMenuItem.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						Customer customer = customerTable.getItems().get(row.getIndex()).getCustomer();
						Main.sessionData.put("customer", customer);
						try {
							utility.openViewAsPopUp("EditCustomer", "Modifier Client");
						} catch (IOException e) {
							e.printStackTrace();
						}	
					}
				});

				return row;
			}
		});
		
		// Add listener on search field		
		searchCustomer.textProperty().addListener((observable, oldValue, newValue) -> {
		    filterCustomerList(oldValue, newValue);
		});
		
		
		//***** ROOM *****
		cRoomNumber.setCellValueFactory(new PropertyValueFactory<Room, String>("room_number"));
		cPrice.setCellValueFactory(new PropertyValueFactory<Room, String>("price"));
		cStatus.setCellValueFactory(new PropertyValueFactory<Room, String>("status"));
		cFloor.setCellValueFactory(new PropertyValueFactory<Room, String>("floor"));
		cSize.setCellValueFactory(new PropertyValueFactory<Room, String>("size"));
		cBeds.setCellValueFactory(new PropertyValueFactory<Room, String>("beds"));	

		// Column TV - Create a Checkbox Cell instead of Simple Boolean property
		cTv.setCellFactory(col -> {
			TableCell<Room, String> cell = new CheckBoxTableCell<>(index -> {
				BooleanProperty active = new SimpleBooleanProperty(roomTable.getItems().get(index).getTv());
				active.addListener((obs, wasActive, isNowActive) -> {
					Room item = roomTable.getItems().get(index);
					item.setTv(isNowActive);
				});
				return active;
			});
			return cell;
		});
		
		// Column Fan - Create a Checkbox Cell instead of Simple Boolean property
		cFan.setCellFactory(col -> {
			TableCell<Room, String> cell = new CheckBoxTableCell<>(index -> {
				BooleanProperty active = new SimpleBooleanProperty(roomTable.getItems().get(index).getFan());
				active.addListener((obs, wasActive, isNowActive) -> {
					Room item = roomTable.getItems().get(index);
					item.setFan(isNowActive);
				});
				return active;
			});
			return cell;
		});

		roomTableData.addAll(roomModel.getAllRoom()); 
		roomTable.setItems(roomTableData);
		roomTable.setRowFactory(new Callback<TableView<Room>, TableRow<Room>>() {
			@Override
			public TableRow<Room> call(TableView<Room> tableView) {
				final TableRow<Room> row = new TableRow<>();
				final ContextMenu contextMenu = new ContextMenu();
				contextMenu.setId("contextMenu");
				final MenuItem deleteMenuItem = new MenuItem("Supprimer");
				final MenuItem editMenuItem = new MenuItem("Modifier");

				// Set context menu on row, but use a binding to make it only show for non-empty rows:
				row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(contextMenu));
				
				contextMenu.getItems().addAll(editMenuItem, deleteMenuItem);

				deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						Alert alert = Tools.showConfirmationDialog("Voulez-vous vraiment supprimer cette chambre?");
						Optional<ButtonType> result = alert.showAndWait();
						if (result.get() == ButtonType.CANCEL) {
							alert.hide();
						} else {
							Room room = roomTable.getItems().get(row.getIndex()).getRoom();
							roomModel.deleteRoom(room.getId_room());
							refreshDataRooms();
						}
					}
				});

				editMenuItem.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						Room room = roomTable.getItems().get(row.getIndex()).getRoom();
						Main.sessionData.put("room", room);
						try {
							utility.openViewAsPopUp("EditRoom", "Modifier la chambre");
						} catch (IOException e) {
							e.printStackTrace();
						}	
					}
				});

				return row;
			}
		});
		
		// Add listener on search field		
		searchRoom.textProperty().addListener((observable, oldValue, newValue) -> {
			filterRoomList(oldValue, newValue);
		});
	}

	public void deconnexion(ActionEvent event) throws IOException {
		// Fermeture de la fenetre
		menuBtn_currentUser.getScene().getWindow().hide();
		utility.openView("Login", "Connexion");		
				
	}
		
	public void filterCustomerList(String oldValue, String newValue){
		ObservableList<Customer> filteredList = FXCollections.observableArrayList();
		if(newValue.equals("")){
			customerTable.setItems(customerTableData);
		}
		else{
			newValue = newValue.toUpperCase();
			for(Customer aCustomer : customerTable.getItems()){
				if(aCustomer.getFirstname().toUpperCase().contains(newValue) 
					|| aCustomer.getLastname().toUpperCase().contains(newValue) 
					|| aCustomer.getId_card().contains(newValue))
				{					
					filteredList.add(aCustomer);
				}
			}
			customerTable.setItems(filteredList);
		}
	}
	
	public void filterRoomList(String oldValue, String newValue){
		ObservableList<Room> filteredList = FXCollections.observableArrayList();
		if(newValue.equals("")){
			roomTable.setItems(roomTableData);
		}
		else{
			newValue = newValue.toUpperCase();
			for(Room aRoom : roomTable.getItems()){
				String roomNumber_STRING = Integer.toString(aRoom.getRoom_number());
				if(roomNumber_STRING.contains(newValue))				{					
					filteredList.add(aRoom);
				}
			}
			roomTable.setItems(filteredList);
		}
	}
	
	@FXML
	private void addCustomer(ActionEvent event) throws IOException {
		Main.sessionData.put("customer", null);
		utility.openViewAsPopUp("EditCustomer", "Nouveau Client");		
	}
	
	@FXML
	private void addRoom() throws IOException {
		Main.sessionData.put("room", null);
		utility.openViewAsPopUp("EditRoom", "Nouvelle chambre");
	}
	
	public static void refreshDataCustomer(){
		customerTableData.clear();
		customerTableData.addAll(customerModel.getAllCustomer()); 
	}
	
	public static void refreshDataRooms(){
		roomTableData.clear();
		roomTableData.addAll(roomModel.getAllRoom()); 
	}

}
