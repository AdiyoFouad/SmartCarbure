package fr.miage.smartcarbure.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.ComparaisonResponse;
import fr.miage.smartcarbure.dto.ZoneComparaisonDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.model.Departement;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.Ville;

/** Compare les prix moyens d'un carburant entre deux villes ou deux départements. */
@Service
public class ComparaisonService {

	private static final int DECIMALES_PRIX = 3;
	private static final Pattern CODE_POSTAL = Pattern.compile("\\d{5}");

	private final CarburantApiClient carburantApiClient;

	public ComparaisonService(CarburantApiClient carburantApiClient) {
		this.carburantApiClient = carburantApiClient;
	}

	public ComparaisonResponse comparer(String type, String zone1, String zone2, Carburant carburant) {
		String typeValide = verifierType(type);
		String premiereZone = verifierZone(zone1, "zone1");
		String secondeZone = verifierZone(zone2, "zone2");
		if (carburant == null) {
			throw new IllegalArgumentException("Le carburant est obligatoire.");
		}

		ZoneComparaisonDTO resultat1 = calculerZone(typeValide, premiereZone, carburant);
		ZoneComparaisonDTO resultat2 = calculerZone(typeValide, secondeZone, carburant);
		BigDecimal ecart = resultat1.prixMoyen() == null || resultat2.prixMoyen() == null
				? null
				: resultat1.prixMoyen().subtract(resultat2.prixMoyen());

		return new ComparaisonResponse(typeValide, carburant, resultat1, resultat2, ecart);
	}

	private ZoneComparaisonDTO calculerZone(String type, String zone, Carburant carburant) {
		List<Station> stations = "VILLE".equals(type)
				? CODE_POSTAL.matcher(zone).matches()
					? carburantApiClient.rechercherParCodePostal(zone)
					: carburantApiClient.rechercherParVille(zone)
				: carburantApiClient.rechercherParDepartement(zone);

		Map<String, Station> stationsUniques = new LinkedHashMap<>();
		if (stations != null) {
			for (Station station : stations) {
				if (station != null) {
					stationsUniques.putIfAbsent(station.getId(), station);
				}
			}
		}

		Map<Carburant, BigDecimal> sommes = new EnumMap<>(Carburant.class);
		Map<Carburant, Integer> comptes = new EnumMap<>(Carburant.class);
		for (Carburant typeCarburant : Carburant.values()) {
			sommes.put(typeCarburant, BigDecimal.ZERO);
			comptes.put(typeCarburant, 0);
		}
		String nomZone = zone;
		for (Station station : stationsUniques.values()) {
			for (Carburant typeCarburant : Carburant.values()) {
				BigDecimal prix = prixDisponible(station, typeCarburant);
				if (prix != null) {
					sommes.put(typeCarburant, sommes.get(typeCarburant).add(prix));
					comptes.put(typeCarburant, comptes.get(typeCarburant) + 1);
				}
			}
			String nomOfficiel = nomOfficiel(station, type);
			if (nomOfficiel != null) {
				nomZone = nomOfficiel;
			}
		}

		Map<Carburant, BigDecimal> moyennes = new EnumMap<>(Carburant.class);
		for (Carburant typeCarburant : Carburant.values()) {
			int nombre = comptes.get(typeCarburant);
			moyennes.put(typeCarburant, nombre == 0 ? null
					: sommes.get(typeCarburant).divide(BigDecimal.valueOf(nombre), DECIMALES_PRIX, RoundingMode.HALF_UP));
		}
		return new ZoneComparaisonDTO(nomZone, moyennes.get(carburant), comptes.get(carburant),
				moyennes, comptes, stationsUniques.size());
	}

	private static BigDecimal prixDisponible(Station station, Carburant carburant) {
		return station.getCarburants().stream()
				.filter(prix -> prix.getCarburant() == carburant)
				.filter(PrixCarburant::isDisponible)
				.map(PrixCarburant::getPrix)
				.filter(prix -> prix != null && prix > 0)
				.findFirst()
				.map(BigDecimal::valueOf)
				.orElse(null);
	}

	private static String nomOfficiel(Station station, String type) {
		if ("VILLE".equals(type)) {
			Ville ville = station.getVille();
			return ville == null || ville.getNom() == null || ville.getNom().isBlank() ? null : ville.getNom();
		}
		Departement departement = station.getDepartement();
		return departement == null ? null : departement.getNom() + " (" + departement.getCode() + ")";
	}

	private static String verifierType(String type) {
		if (type == null || type.isBlank()) {
			throw new IllegalArgumentException("Le type de comparaison est obligatoire.");
		}
		String valeur = type.trim().toUpperCase(Locale.ROOT);
		if (!"VILLE".equals(valeur) && !"DEPARTEMENT".equals(valeur)) {
			throw new IllegalArgumentException("Le type doit être VILLE ou DEPARTEMENT.");
		}
		return valeur;
	}

	private static String verifierZone(String zone, String nomParametre) {
		if (zone == null || zone.isBlank()) {
			throw new IllegalArgumentException("Le paramètre " + nomParametre + " est obligatoire.");
		}
		return zone.trim();
	}
}
