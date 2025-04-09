package com.project.dao;

import com.project.datasource.DataSource;
import com.project.model.Projekt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjektDAOImpl implements ProjektDAO {

    @Override
    public Projekt getProjekt(Integer projektId) {
        String query = "SELECT * FROM projekt WHERE projekt_id = ?";
        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, projektId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    Projekt p = new Projekt();
                    p.setProjektId(rs.getInt("projekt_id"));
                    p.setNazwa(rs.getString("nazwa"));
                    p.setOpis(rs.getString("opis"));
                    p.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
                    p.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
                    return p;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setProjekt(Projekt projekt) {
        boolean isInsert = (projekt.getProjektId() == null);
        String query = isInsert
                ? "INSERT INTO projekt(nazwa, opis, dataczas_utworzenia, data_oddania) VALUES (?, ?, ?, ?)"
                : "UPDATE projekt SET nazwa = ?, opis = ?, dataczas_utworzenia = ?, data_oddania = ? WHERE projekt_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            // Wypełnienie znaków '?'
            prepStmt.setString(1, projekt.getNazwa());
            prepStmt.setString(2, projekt.getOpis());
            if (projekt.getDataCzasUtworzenia() == null) {
                // jeśli brak daty utworzenia, ustaw ją na teraz
                projekt.setDataCzasUtworzenia(LocalDateTime.now());
            }
            prepStmt.setObject(3, projekt.getDataCzasUtworzenia());
            prepStmt.setObject(4, projekt.getDataOddania());

            if (!isInsert) {
                prepStmt.setInt(5, projekt.getProjektId());
            }

            int liczbaDodanychWierszy = prepStmt.executeUpdate();

            // Dla INSERT pobieramy wygenerowany klucz
            if (isInsert && liczbaDodanychWierszy > 0) {
                try (ResultSet keys = prepStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        projekt.setProjektId(keys.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteProjekt(Integer projektId) {
        String query = "DELETE FROM projekt WHERE projekt_id = ?";
        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, projektId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Projekt> getProjekty(Integer offset, Integer limit) {
        List<Projekt> projekty = new ArrayList<>();

        String query = "SELECT * FROM projekt ORDER BY dataczas_utworzenia DESC"
                + (offset != null ? " OFFSET ?" : "")
                + (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            if (offset != null) {
                prepStmt.setInt(i++, offset);
            }
            if (limit != null) {
                prepStmt.setInt(i++, limit);
            }

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    Projekt p = new Projekt();
                    p.setProjektId(rs.getInt("projekt_id"));
                    p.setNazwa(rs.getString("nazwa"));
                    p.setOpis(rs.getString("opis"));
                    p.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
                    p.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
                    projekty.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projekty;
    }

    @Override
    public List<Projekt> getProjektyWhereNazwaLike(String nazwa, Integer offset, Integer limit) {
        List<Projekt> projekty = new ArrayList<>();

        String query = "SELECT * FROM projekt WHERE LOWER(nazwa) LIKE LOWER(?)"
                + " ORDER BY dataczas_utworzenia DESC"
                + (offset != null ? " OFFSET ?" : "")
                + (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            prepStmt.setString(i++, "%" + nazwa + "%");

            if (offset != null) {
                prepStmt.setInt(i++, offset);
            }
            if (limit != null) {
                prepStmt.setInt(i++, limit);
            }

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    Projekt p = new Projekt();
                    p.setProjektId(rs.getInt("projekt_id"));
                    p.setNazwa(rs.getString("nazwa"));
                    p.setOpis(rs.getString("opis"));
                    p.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
                    p.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
                    projekty.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projekty;
    }

    @Override
    public List<Projekt> getProjektyWhereDataOddaniaIs(LocalDate dataOddania, Integer offset, Integer limit) {
        List<Projekt> projekty = new ArrayList<>();

        String query = "SELECT * FROM projekt WHERE data_oddania = ?"
                + " ORDER BY dataczas_utworzenia DESC"
                + (offset != null ? " OFFSET ?" : "")
                + (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            prepStmt.setObject(i++, dataOddania);
            if (offset != null) {
                prepStmt.setInt(i++, offset);
            }
            if (limit != null) {
                prepStmt.setInt(i++, limit);
            }

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    Projekt p = new Projekt();
                    p.setProjektId(rs.getInt("projekt_id"));
                    p.setNazwa(rs.getString("nazwa"));
                    p.setOpis(rs.getString("opis"));
                    p.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
                    p.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
                    projekty.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projekty;
    }

    @Override
    public int getRowsNumber() {
        String query = "SELECT COUNT(*) AS cnt FROM projekt";
        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query);
             ResultSet rs = prepStmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("cnt");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRowsNumberWhereNazwaLike(String nazwa) {
        String query = "SELECT COUNT(*) AS cnt FROM projekt WHERE LOWER(nazwa) LIKE LOWER(?)";
        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setString(1, "%" + nazwa + "%");
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRowsNumberWhereDataOddaniaIs(LocalDate dataOddania) {
        String query = "SELECT COUNT(*) AS cnt FROM projekt WHERE data_oddania = ?";
        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setObject(1, dataOddania);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
