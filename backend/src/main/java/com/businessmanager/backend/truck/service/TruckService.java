package com.businessmanager.backend.truck.service;

import com.businessmanager.backend.truck.dto.TruckExpensePostingRequest;
import com.businessmanager.backend.truck.entity.Truck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TruckService {
    Truck createTruck(Truck truck);
    Truck getTruckById(Long id);
    Truck getTruckByRegistrationNumber(String registrationNumber);
    Truck updateTruck(Long id, Truck truckDetails);
    void deleteTruck(Long id);
    Page<Truck> searchTrucks(String registrationNumber, String make, String model, Long driverId, Pageable pageable);

    boolean isMaintenanceDue(Long truckId, Integer customIntervalDays);
    List<Truck> getTrucksDueForMaintenance(Integer customIntervalDays);
    void postTruckExpense(TruckExpensePostingRequest request);
}

