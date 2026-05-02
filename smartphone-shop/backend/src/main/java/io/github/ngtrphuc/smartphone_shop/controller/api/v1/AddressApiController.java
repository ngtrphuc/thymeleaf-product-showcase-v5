package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.api.dto.AddressResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.OperationStatusResponse;
import io.github.ngtrphuc.smartphone_shop.model.Address;
import io.github.ngtrphuc.smartphone_shop.service.AddressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressApiController {

    private final AddressService addressService;
    private final ApiMapper apiMapper;

    public AddressApiController(AddressService addressService, ApiMapper apiMapper) {
        this.addressService = addressService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public List<AddressResponse> list(org.springframework.security.core.Authentication authentication) {
        return addressService.listUserAddresses(authentication.getName()).stream()
                .map(apiMapper::toAddressResponse)
                .toList();
    }

    @PostMapping
    public AddressResponse create(@Valid @RequestBody UpsertAddressRequest request,
            org.springframework.security.core.Authentication authentication) {
        Address created = addressService.createAddress(authentication.getName(), request.toCommand());
        return apiMapper.toAddressResponse(created);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable("id") Long id,
            @Valid @RequestBody UpsertAddressRequest request,
            org.springframework.security.core.Authentication authentication) {
        Address updated = addressService.updateAddress(authentication.getName(), id, request.toCommand());
        return apiMapper.toAddressResponse(updated);
    }

    @DeleteMapping("/{id}")
    public OperationStatusResponse delete(@PathVariable("id") Long id,
            org.springframework.security.core.Authentication authentication) {
        addressService.deleteAddress(authentication.getName(), id);
        return new OperationStatusResponse(true, "Address deleted.");
    }

    @PutMapping("/{id}/default")
    public AddressResponse setDefault(@PathVariable("id") Long id,
            org.springframework.security.core.Authentication authentication) {
        Address updated = addressService.setDefaultAddress(authentication.getName(), id);
        return apiMapper.toAddressResponse(updated);
    }

    private record UpsertAddressRequest(
            @Size(max = 50, message = "Address label is too long.")
            String label,
            @NotBlank(message = "Recipient name is required.")
            @Size(max = 100, message = "Recipient name is too long.")
            String recipientName,
            @Size(max = 20, message = "Phone number is too long.")
            String phoneNumber,
            @Size(max = 10, message = "Postal code is too long.")
            String postalCode,
            @Size(max = 30, message = "Prefecture is too long.")
            String prefecture,
            @Size(max = 50, message = "City is too long.")
            String city,
            @NotBlank(message = "Street address is required.")
            @Size(max = 200, message = "Street address is too long.")
            String streetAddress,
            @Size(max = 100, message = "Building is too long.")
            String building,
            boolean isDefault) {
        AddressService.AddressCommand toCommand() {
            return new AddressService.AddressCommand(
                    label,
                    recipientName,
                    phoneNumber,
                    postalCode,
                    prefecture,
                    city,
                    streetAddress,
                    building,
                    isDefault);
        }
    }
}
