package com.project.controller;

import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.model.Projekt;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProjectController {

	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

	// Formatery dat
	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	// Pola do obsługi wyszukiwania i paginacji
	private String search4;    // co wyszukujemy
	private Integer pageNo;    // numer strony
	private Integer pageSize;  // rozmiar strony (ile rekordów na stronie)

	// DAO i wątek
	private ExecutorService wykonawca;
	private ProjektDAO projektDAO;

	// Kolekcja obserwowalna z listą projektów
	private ObservableList<Projekt> projekty;

	// ================= @FXML KOMPONENTY Z FXML ===============
	@FXML
	private TableView<Projekt> tblProjekt;

	@FXML
	private TableColumn<Projekt, Integer> colId;

	@FXML
	private TableColumn<Projekt, String> colNazwa;

	@FXML
	private TableColumn<Projekt, String> colOpis;

	@FXML
	private TableColumn<Projekt, LocalDateTime> colDataCzasUtworzenia;

	@FXML
	private TableColumn<Projekt, LocalDate> colDataOddania;

	@FXML
	private TextField txtSzukaj;

	@FXML
	private ChoiceBox<Integer> cbPageSizes;

	// Przykładowe przyciski do paginacji
	@FXML
	private Button btnDalej;

	@FXML
	private Button btnWstecz;

	@FXML
	private Button btnDodaj;

	//opcjonalne:
	@FXML
	private Button btnPierwsza;

	@FXML
	private Button btnOstatnia;

	// ================== KONSTRUKTORY ===============
	public ProjectController() {
		// Konstruktor bezparametrowy -> JavaFX tworzy kontroler przez reflection
		// (jeśli używasz setControllerFactory, możesz też wstrzykiwać DAO do innego konstruktora).
	}

	public ProjectController(ProjektDAO projektDAO) {
		// Konstruktor wywoływany, gdy sam tworzysz kontroler i przekazujesz DAO
		// np. w ProjectClientApplication -> loader.setControllerFactory(...).
		this.projektDAO = projektDAO;
		this.wykonawca = Executors.newFixedThreadPool(1);
	}

	// =============== METODA INITIALIZE ==================
	@FXML
	public void initialize() {
		// Ustawienia początkowe wyszukiwania/paginacji
		search4 = "";
		pageNo = 0;
		pageSize = 10;

		// Wypełnij ChoiceBox możliwymi rozmiarami strony
		cbPageSizes.getItems().addAll(5, 10, 20, 50, 100);
		cbPageSizes.setValue(pageSize);

		// Inicjalizacja kolumn w tabeli
		colId.setCellValueFactory(new PropertyValueFactory<>("projektId"));
		colNazwa.setCellValueFactory(new PropertyValueFactory<>("nazwa"));
		colOpis.setCellValueFactory(new PropertyValueFactory<>("opis"));
		colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<>("dataCzasUtworzenia"));
		colDataOddania.setCellValueFactory(new PropertyValueFactory<>("dataOddania"));

		// Formatowanie wyświetlania daty/czasu w kolumnie
		colDataCzasUtworzenia.setCellFactory(col -> new TableCell<Projekt, LocalDateTime>() {
			@Override
			protected void updateItem(LocalDateTime item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(dateTimeFormatter.format(item));
				}
			}
		});
		// W analogiczny sposób mógłbyś sformatować LocalDate:
		colDataOddania.setCellFactory(col -> new TableCell<Projekt, LocalDate>() {
			@Override
			protected void updateItem(LocalDate item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(dateFormatter.format(item));
				}
			}
		});

		// Tworzymy listę obserwowalną i przypisujemy do tabeli
		projekty = FXCollections.observableArrayList();
		tblProjekt.setItems(projekty);

		// Opcjonalnie: Dodanie kolumny z przyciskami "Edycja", "Usuń", "Zadania"
		addEditButtonsColumn();

		// Jeżeli nie wstrzyknięto DAO w konstruktorze, można utworzyć tu:
		if (this.projektDAO == null) {
			this.projektDAO = new ProjektDAOImpl();
			this.wykonawca = Executors.newFixedThreadPool(1);
		}

		// Pierwsze pobranie danych (strona 0)
		wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
	}

	// ============== METODY OBSŁUGI ZDARZEŃ =============
	@FXML
	private void onActionBtnSzukaj(ActionEvent event) {
		logger.info("Szukam projektów wg frazy: {}", txtSzukaj.getText());
		search4 = txtSzukaj.getText().trim();
		pageNo = 0;  // zaczynamy od strony 0
		pageSize = cbPageSizes.getValue();
		wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
	}

	@FXML
	private void onActionBtnDalej(ActionEvent event) {
		pageNo++;
		pageSize = cbPageSizes.getValue();
		wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
	}

	@FXML
	private void onActionBtnWstecz(ActionEvent event) {
		if (pageNo > 0) {
			pageNo--;
			pageSize = cbPageSizes.getValue();
			wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
		}
	}

	@FXML
	private void onActionBtnPierwsza(ActionEvent event) {
		// (opcjonalne) Idź do pierwszej strony
		pageNo = 0;
		pageSize = cbPageSizes.getValue();
		wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
	}

	@FXML
	private void onActionBtnOstatnia(ActionEvent event) {
		// (opcjonalne) np. oblicz liczbę stron = total / pageSize
		// i przeskocz do ostatniej strony
		// Poniżej wersja symboliczna (bez obliczania):
		pageNo = 999999; // hack: i tak baza pewnie zwróci pustą odpowiedź,
		pageSize = cbPageSizes.getValue();
		wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
	}

	@FXML
	private void onActionBtnDodaj(ActionEvent event) {
		edytujProjekt(new Projekt());
	}

	// ============ METODY POMOCNICZE =============

	/**
	 * Ładuje stronę projektów (w tle) i aktualizuje tabelę w JavaFX
	 */
	private void loadPage(String search4, int pageNo, int pageSize) {
		try {
			List<Projekt> projektList = new ArrayList<>();
			// Ewentualnie wykryj, czy search4 to ID albo data
			if (search4 != null && !search4.isEmpty()) {
				// uproszczony wariant: wyszukujemy po nazwie
				projektList.addAll(projektDAO.getProjektyWhereNazwaLike(search4, pageNo * pageSize, pageSize));
			} else {
				// pobieramy wszystkie
				projektList.addAll(projektDAO.getProjekty(pageNo * pageSize, pageSize));
			}
			// Przekazujemy do GUI
			Platform.runLater(() -> {
				projekty.clear();
				projekty.addAll(projektList);
			});
		} catch (RuntimeException e) {
			logger.error("Błąd podczas pobierania listy projektów.", e);
			String msg = "Błąd podczas pobierania listy projektów:\n" + e.getMessage();
			Platform.runLater(() -> showError("Błąd", msg));
		}
	}

	/**
	 * Prosta metoda pomocnicza wyświetlająca okienko z błędem
	 */
	private void showError(String header, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Błąd");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * Dodaje do tabeli nową kolumnę z przyciskami (Edycja, Usuń, Zadania).
	 */
	private void addEditButtonsColumn() {
		TableColumn<Projekt, Void> colEdit = new TableColumn<>("Akcje");
		colEdit.setCellFactory(param -> new TableCell<>() {
			private final Button btnEdit = new Button("Edycja");
			private final Button btnRemove = new Button("Usuń");
			private final Button btnTask = new Button("Zadania");
			private final GridPane pane = new GridPane();

			{
				// Konfiguracja przycisków
				btnEdit.setMaxWidth(Double.MAX_VALUE);
				btnRemove.setMaxWidth(Double.MAX_VALUE);
				btnTask.setMaxWidth(Double.MAX_VALUE);

				// Obsługa zdarzeń
				btnEdit.setOnAction(e -> {
					Projekt p = getCurrentProjekt();
					if (p != null) {
						edytujProjekt(p);
					}
				});
				btnRemove.setOnAction(e -> {
					Projekt p = getCurrentProjekt();
					if (p != null) {
						usunProjekt(p);
					}
				});
				btnTask.setOnAction(e -> {
					Projekt p = getCurrentProjekt();
					if (p != null) {
						// TODO openZadanieFrame(p); - okno zadań
						logger.info("Zadania dla projektu: {}", p.getProjektId());
					}
				});

				// Ustawiamy layout w GridPane
				pane.setAlignment(Pos.CENTER);
				pane.setHgap(5);
				pane.setVgap(5);
				pane.setPadding(new Insets(5));
				pane.add(btnEdit, 0, 0);
				pane.add(btnRemove, 0, 1);
				pane.add(btnTask, 0, 2);
			}

			private Projekt getCurrentProjekt() {
				int index = getTableRow().getIndex();
				return getTableView().getItems().get(index);
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					setGraphic(pane);
				}
			}
		});
		tblProjekt.getColumns().add(colEdit);

		// Możesz ustawić szerokości kolumn
		// colEdit.setMaxWidth(1000);
	}

	/**
	 * Otwiera dialog edycji/dodania projektu
	 */
	private void edytujProjekt(Projekt projekt) {
		Dialog<Projekt> dialog = new Dialog<>();
		dialog.setTitle(projekt.getProjektId() == null ? "Dodawanie projektu" : "Edycja projektu");
		dialog.setHeaderText("Wprowadź dane projektu");
		dialog.setResizable(true);

		// Etykiety
		Label lblId = new Label("ID:");
		Label lblNazwa = new Label("Nazwa:");
		Label lblOpis = new Label("Opis:");
		Label lblDataOddania = new Label("Data oddania (RRRR-MM-DD):");
		Label lblDataCzasUtworzenia = new Label("Data utworzenia:");

		// Pola tekstowe / DatePicker
		Label txtId = new Label(projekt.getProjektId() == null ? "" : projekt.getProjektId().toString());

		TextField txtNazwa = new TextField(projekt.getNazwa() != null ? projekt.getNazwa() : "");

		TextArea txtOpis = new TextArea(projekt.getOpis() != null ? projekt.getOpis() : "");
		txtOpis.setPrefRowCount(5);
		txtOpis.setWrapText(true);

		Label txtDataCzas = new Label();
		if (projekt.getDataCzasUtworzenia() != null) {
			txtDataCzas.setText(dateTimeFormatter.format(projekt.getDataCzasUtworzenia()));
		}

		DatePicker dpDataOddania = new DatePicker(projekt.getDataOddania());
		dpDataOddania.setPromptText("RRRR-MM-DD");
		dpDataOddania.setConverter(new StringConverter<>() {
			@Override
			public String toString(LocalDate date) {
				return date == null ? "" : dateFormatter.format(date);
			}

			@Override
			public LocalDate fromString(String text) {
				if (text == null || text.isEmpty()) return null;
				return LocalDate.parse(text, dateFormatter);
			}
		});
		dpDataOddania.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused) {
				// Gdy wychodzimy z pola, próbujemy sparsować
				try {
					LocalDate val = dpDataOddania.getConverter().fromString(dpDataOddania.getEditor().getText());
					dpDataOddania.setValue(val);
				} catch (DateTimeParseException e) {
					dpDataOddania.getEditor().setText(
							dpDataOddania.getConverter().toString(dpDataOddania.getValue())
					);
				}
			}
		});

		// Układ w GridPane
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10));
		grid.add(lblId, 0, 0);              grid.add(txtId, 1, 0);
		grid.add(lblDataCzasUtworzenia, 0, 1); grid.add(txtDataCzas, 1, 1);
		grid.add(lblNazwa, 0, 2);          grid.add(txtNazwa, 1, 2);
		grid.add(lblOpis, 0, 3);           grid.add(txtOpis, 1, 3);
		grid.add(lblDataOddania, 0, 4);    grid.add(dpDataOddania, 1, 4);

		dialog.getDialogPane().setContent(grid);

		// Przyciski OK/Cancel
		ButtonType btnOk = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
		ButtonType btnCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);

		// Konwertujemy wynik dialogu na Projekt
		dialog.setResultConverter(buttonType -> {
			if (buttonType == btnOk) {
				projekt.setNazwa(txtNazwa.getText().trim());
				projekt.setOpis(txtOpis.getText().trim());
				projekt.setDataOddania(dpDataOddania.getValue());
				return projekt;
			}
			return null;
		});

		// Wyświetlamy dialog i czekamy na rezultat
		Optional<Projekt> result = dialog.showAndWait();
		if (result.isPresent()) {
			// Użytkownik wybrał 'Zapisz'
			Projekt p = result.get();
			wykonawca.execute(() -> {
				try {
					projektDAO.setProjekt(p);
					// Po zapisaniu odświeżamy w GUI
					Platform.runLater(() -> {
						if (!tblProjekt.getItems().contains(p)) {
							// Dodaj na początek listy
							tblProjekt.getItems().add(0, p);
						} else {
							// Odśwież istniejący
							tblProjekt.refresh();
						}
					});
				} catch (RuntimeException e) {
					logger.error("Błąd podczas zapisywania projektu", e);
					Platform.runLater(() -> showError("Błąd zapisu", e.getMessage()));
				}
			});
		}
	}

	/**
	 * Usunięcie projektu z bazy
	 */
	private void usunProjekt(Projekt projekt) {
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Usunięcie projektu");
		confirm.setHeaderText("Czy na pewno chcesz usunąć projekt o ID=" + projekt.getProjektId() + "?");
		confirm.setContentText("Operacja nieodwracalna.");

		Optional<ButtonType> res = confirm.showAndWait();
		if (res.isPresent() && res.get() == ButtonType.OK) {
			wykonawca.execute(() -> {
				try {
					projektDAO.deleteProjekt(projekt.getProjektId());
					Platform.runLater(() -> {
						tblProjekt.getItems().remove(projekt);
					});
				} catch (RuntimeException e) {
					logger.error("Błąd podczas usuwania projektu", e);
					Platform.runLater(() -> showError("Błąd usuwania", e.getMessage()));
				}
			});
		}
	}

	/**
	 * Zamykanie puli wątków (wywoływane np. przy zamknięciu okna)
	 */
	public void shutdown() {
		if (wykonawca != null) {
			wykonawca.shutdown();
			try {
				if (!wykonawca.awaitTermination(5, TimeUnit.SECONDS)) {
					wykonawca.shutdownNow();
				}
			} catch (InterruptedException e) {
				wykonawca.shutdownNow();
			}
		}
	}
}
