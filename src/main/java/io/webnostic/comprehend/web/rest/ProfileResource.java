package io.webnostic.comprehend.web.rest;

import com.codahale.metrics.annotation.Timed;
import io.webnostic.comprehend.domain.Profile;

import io.webnostic.comprehend.repository.ProfileRepository;
import io.webnostic.comprehend.service.AmazonClient;
import io.webnostic.comprehend.web.rest.errors.BadRequestAlertException;
import io.webnostic.comprehend.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import jdk.nashorn.internal.runtime.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.ws.rs.Produces;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for managing Profile.
 */
@RestController
@RequestMapping("/api")
public class ProfileResource {

    private final Logger log = LoggerFactory.getLogger(ProfileResource.class);

    private static final String ENTITY_NAME = "profile";

    private final ProfileRepository profileRepository;

    private AmazonClient amazonClient;

    private static final String DYNAMODB_BASE_URI_V1 = "https://ghkaxu7qyj.execute-api.us-east-1.amazonaws.com/beta/";

    private RestTemplate rest;
    private HttpHeaders headers;
    private HttpStatus status;


    @Autowired
    public ProfileResource(ProfileRepository profileRepository, AmazonClient amazonClient) {

        this.profileRepository = profileRepository;
        this.amazonClient = amazonClient;

        this.rest = new RestTemplate();
        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "*/*");
    }

    /**
     * POST  /profiles : Create a new profile.
     *
     * @param profile the profile to create
     * @return the ResponseEntity with status 201 (Created) and with body the new profile, or with status 400 (Bad Request) if the profile has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/profiles")
    @Timed
    public ResponseEntity<Profile> createProfile(@RequestBody Profile profile) throws URISyntaxException {
        log.debug("REST request to save Profile : {}", profile);
        if (profile.getId() != null) {
            throw new BadRequestAlertException("A new profile cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Profile result = profileRepository.save(profile);
        return ResponseEntity.created(new URI("/api/profiles/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /profiles : Updates an existing profile.
     *
     * @param profile the profile to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated profile,
     * or with status 400 (Bad Request) if the profile is not valid,
     * or with status 500 (Internal Server Error) if the profile couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/profiles")
    @Timed
    public ResponseEntity<Profile> updateProfile(@RequestBody Profile profile) throws URISyntaxException {
        log.debug("REST request to update Profile : {}", profile);
        if (profile.getId() == null) {
            return createProfile(profile);
        }
        Profile result = profileRepository.save(profile);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, profile.getId().toString()))
            .body(result);
    }

    /**
     * GET  /profiles : get all the profiles.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of profiles in body
     */
    @GetMapping("/profiles")
    @Timed
    public List<Profile> getAllProfiles() {
        log.debug("REST request to get all Profiles");
        return profileRepository.findAll();
    }

    /**
     * GET  /profiles/:id : get the "id" profile.
     *
     * @param id the id of the profile to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the profile, or with status 404 (Not Found)
     */
    @GetMapping("/profiles/{id}")
    @Timed
    public ResponseEntity<Profile> getProfile(@PathVariable Long id) {
        log.debug("REST request to get Profile : {}", id);
        Profile profile = profileRepository.findOne(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(profile));
    }

    /**
     * DELETE  /profiles/:id : delete the "id" profile.
     *
     * @param id the id of the profile to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/profiles/{id}")
    @Timed
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        log.debug("REST request to delete Profile : {}", id);
        profileRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestPart(value = "file") MultipartFile file) {
        return this.amazonClient.uploadFile(file);
    }

    @PostMapping("/uploadAudio")
    public String uploadAudio(@RequestPart(value = "audio") MultipartFile audio) {
        return this.amazonClient.uploadAudio(audio);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/getComprehendResults/")
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    public String getComprehendResults(@RequestParam(value = "tablename") String tableName,
                                                  @RequestParam(value = "id") String id) throws IOException {
        String uri = "microComprehend?tablename=" + tableName +"&id=" + id;
        HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(DYNAMODB_BASE_URI_V1 + uri, HttpMethod.GET, requestEntity, String.class);
        return responseEntity.getBody();

    }
}
