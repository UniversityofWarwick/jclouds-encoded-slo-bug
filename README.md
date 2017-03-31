# JClouds multipart SLO Swift bug

This repository demonstrates an apparent bug in JClouds when PUTing a multipart static large object (SLO) to Openstack
Swift, where the key for that blob contains encoded characters - these are double-encoded when creating the manifest file
and as such the final PUT which creates the manifest file fails.

## Steps to reproduce

### Run a Swift installation (e.g. with Devstack)

We used [a Vagrant devstack install](https://github.com/openstack-dev/devstack-vagrant) with the default config. We had
to make one change due to the latest Devstack not supporting Ubuntu trusty (at the time of writing) to set FORCE=true in
the Vagrantfile before provisioning.

    diff --git a/Vagrantfile b/Vagrantfile
    index 1806543..6b102cd 100644
    --- a/Vagrantfile
    +++ b/Vagrantfile
    @@ -78,7 +78,7 @@ def configure_vm(name, vm, conf)

       if conf['setup_mode'] == "devstack"
         vm.provision "shell" do |shell|
    -      shell.inline = "sudo su - stack -c 'cd ~/devstack && ./stack.sh'"
    +      shell.inline = "sudo su - stack -c 'cd ~/devstack && FORCE=yes ./stack.sh'"
         end
       end

Our sample config.yaml (setting passwords and enabling Swift at the bottom):

    hostname_manager: manager.yoursite.com
    hostname_compute: compute.yoursite.com

    user_domains: .yoursite.com

    stack_password: secretadmin
    service_password: secretadmin
    admin_password: secretadmin

    stack_sshkey:

    setup_mode: devstack

    bridge_int: eth1

    manager_extra_services: s-proxy s-object s-container s-account

### Set Swift authentication properties

You can edit `src/test/resources/swift.properties` to put the correct credentials in before running the tests.

### Run the tests

Run `./gradlew test`. This will run the tests for a transient, openstack-swift and filesystem blob store - you should
see that the only test that fails is `SwiftJCloudsSLOTest.sloWithEncodedChars` - this will fail with an error at the final
stage of the SLO upload because the manifest file will be rejected by the Swift server.

## Sample output

### `SwiftJCloudsSLOTest.sloWithSpaces`

This test uses a key of `Files/OpenOffice.org 3.3 (en-GB) Installation Files/openofficeorg1.cab`

    15907 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%28en-GB%29%20Installation%20Files/openofficeorg1.cab/slo/1490950143.154000/38547913/0/00000001 HTTP/1.1
    ...
    15908 DEBUG jclouds.headers >> Content-Type: application/unknown
    15908 DEBUG jclouds.headers >> Content-Length: 33554432
    15908 DEBUG jclouds.headers >> Content-Disposition: Files/OpenOffice.org 3.3 (en-GB) Installation Files/openofficeorg1.cab
    16085 DEBUG jclouds.headers << HTTP/1.1 201 Created
    ...
    17953 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%28en-GB%29%20Installation%20Files/openofficeorg1.cab/slo/1490950143.154000/38547913/0/00000002 HTTP/1.1
    ...
    17953 DEBUG jclouds.headers >> Content-Type: application/unknown
    17953 DEBUG jclouds.headers >> Content-Length: 4993481
    17953 DEBUG jclouds.headers >> Content-Disposition: Files/OpenOffice.org 3.3 (en-GB) Installation Files/openofficeorg1.cab
    17990 DEBUG jclouds.headers << HTTP/1.1 201 Created
    ...
    18009 DEBUG jclouds.wire >> "[{"path":"slo-test/Files/OpenOffice.org 3.3 (en-GB) Installation Files/openofficeorg1.cab/slo/1490950143.154000/38547913/0/00000001","etag":"58f06dd588d8ffb3beb46ada6309436b","size_bytes":33554432},{"path":"slo-test/Files/OpenOffice.org 3.3 (en-GB) Installation Files/openofficeorg1.cab/slo/1490950143.154000/38547913/0/00000002","etag":"2b4b81733d0a2e4abe89516639627408","size_bytes":4993481}]"
    18009 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%28en-GB%29%20Installation%20Files/openofficeorg1.cab?multipart-manifest=put HTTP/1.1
    ...
    18010 DEBUG jclouds.headers >> Content-Type: application/unknown
    18010 DEBUG jclouds.headers >> Content-Length: 394
    18010 DEBUG jclouds.headers >> Content-Disposition: Files/OpenOffice.org 3.3 (en-GB) Installation Files/openofficeorg1.cab
    18029 DEBUG jclouds.headers << HTTP/1.1 201 Created
    ...

### `SwiftJCloudsSLOTest.sloWithEncodedChars`

This test uses a key of `Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab`

    12602 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950292.028000/38547913/0/00000001 HTTP/1.1
    ...
    12603 DEBUG jclouds.headers >> Content-Type: application/unknown
    12603 DEBUG jclouds.headers >> Content-Length: 33554432
    12603 DEBUG jclouds.headers >> Content-Disposition: Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab
    12779 DEBUG jclouds.headers << HTTP/1.1 201 Created
    ...
    14715 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950292.028000/38547913/0/00000002 HTTP/1.1
    ...
    14716 DEBUG jclouds.headers >> Content-Type: application/unknown
    14716 DEBUG jclouds.headers >> Content-Length: 4993481
    14716 DEBUG jclouds.headers >> Content-Disposition: Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab
    14761 DEBUG jclouds.headers << HTTP/1.1 201 Created
    ...
    14780 DEBUG jclouds.wire >> "[{"path":"slo-test/Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab/slo/1490950292.028000/38547913/0/00000001","etag":"58f06dd588d8ffb3beb46ada6309436b","size_bytes":33554432},{"path":"slo-test/Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab/slo/1490950292.028000/38547913/0/00000002","etag":"2b4b81733d0a2e4abe89516639627408","size_bytes":4993481}]"
    14781 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab?multipart-manifest=put HTTP/1.1
    ...
    14781 DEBUG jclouds.headers >> Content-Type: application/unknown
    14781 DEBUG jclouds.headers >> Content-Length: 402
    14781 DEBUG jclouds.headers >> Content-Disposition: Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab
    14794 DEBUG jclouds.headers << HTTP/1.1 400 Bad Request
    14794 DEBUG jclouds.headers << Connection: keep-alive
    ...
    14794 DEBUG jclouds.headers << Content-Type: application/json
    14794 DEBUG jclouds.headers << Content-Length: 332
    14794 DEBUG jclouds.wire << "{"Errors": [["slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950292.028000/38547913/0/00000002", "404 Not Found"], ["slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950292.028000/38547913/0/00000001", "404 Not Found"]]}"

The response from Swift here is strange, the path that it gives the 404 for is identical to the path that it was originally PUT to.

## Behaviour in `python-swiftclient`

This is the reference implementation of a Swift client and fails in the same way as JClouds does.

    $ sudo pip install python-swiftclient python-keystoneclient
    ...
    $ fallocate -l 38547913 large.file

    $ swift --debug \
        --os-auth-url http://137.205.194.8:5000/identity/v2.0 \
        --os-tenant-name demo \
        --os-username demo \
        --os-password secretadmin \
        upload --use-slo --segment-size 33554432 \
        --object-name 'Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab' \
        slo-test large.file
    DEBUG:keystoneclient.auth.identity.v2:Making authentication request to http://137.205.194.8:5000/identity/v2.0/tokens
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:5000 "POST /identity/v2.0/tokens HTTP/1.1" 200 2922
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test HTTP/1.1" 201 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test -X PUT -H "Content-Length: 0" -H "X-Auth-Token: gAAAAABY3h6V4qpA2BQvZ_kOewaaHrUNGd-9XocS6OqjMyYEe598hUGa6u6O6PVrtBFRoV6rxhpEU8hMNd2QlMAkt2V0nhbR1YAY2J3f7SHaCNb-8LXapgswUZUo2S4uQsqcKSPdDS60rXAvg6O5nfAQOZB0cSjQC--TMpOWl0SSekVD9j__bwY"
    DEBUG:swiftclient:RESP STATUS: 201 Created
    DEBUG:swiftclient:RESP HEADERS: {u'Date': u'Fri, 31 Mar 2017 09:17:09 GMT', u'Content-Length': u'0', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'tx00c401abd2064765b17ee-0058de1e95', u'X-Trans-Id': u'tx00c401abd2064765b17ee-0058de1e95'}
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "HEAD /v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test HTTP/1.1" 204 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test -I -H "X-Auth-Token: gAAAAABY3h6V4qpA2BQvZ_kOewaaHrUNGd-9XocS6OqjMyYEe598hUGa6u6O6PVrtBFRoV6rxhpEU8hMNd2QlMAkt2V0nhbR1YAY2J3f7SHaCNb-8LXapgswUZUo2S4uQsqcKSPdDS60rXAvg6O5nfAQOZB0cSjQC--TMpOWl0SSekVD9j__bwY"
    DEBUG:swiftclient:RESP STATUS: 204 No Content
    DEBUG:swiftclient:RESP HEADERS: {u'Content-Length': u'0', u'X-Container-Object-Count': u'0', u'Date': u'Fri, 31 Mar 2017 09:17:09 GMT', u'Accept-Ranges': u'bytes', u'X-Storage-Policy': u'Policy-0', u'Last-Modified': u'Fri, 31 Mar 2017 09:17:10 GMT', u'X-Timestamp': u'1490949982.14393', u'X-Trans-Id': u'tx8ad281c8f3274a80a6f52-0058de1e95', u'X-Container-Bytes-Used': u'0', u'Content-Type': u'text/plain; charset=utf-8', u'X-Openstack-Request-Id': u'tx8ad281c8f3274a80a6f52-0058de1e95'}
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test_segments HTTP/1.1" 201 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test_segments -X PUT -H "Content-Length: 0" -H "X-Storage-Policy: Policy-0" -H "X-Auth-Token: gAAAAABY3h6V4qpA2BQvZ_kOewaaHrUNGd-9XocS6OqjMyYEe598hUGa6u6O6PVrtBFRoV6rxhpEU8hMNd2QlMAkt2V0nhbR1YAY2J3f7SHaCNb-8LXapgswUZUo2S4uQsqcKSPdDS60rXAvg6O5nfAQOZB0cSjQC--TMpOWl0SSekVD9j__bwY"
    DEBUG:swiftclient:RESP STATUS: 201 Created
    DEBUG:swiftclient:RESP HEADERS: {u'Date': u'Fri, 31 Mar 2017 09:17:09 GMT', u'Content-Length': u'0', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'tx8e74e42317914f769902d-0058de1e95', u'X-Trans-Id': u'tx8e74e42317914f769902d-0058de1e95'}
    DEBUG:keystoneclient.auth.identity.v2:Making authentication request to http://137.205.194.8:5000/identity/v2.0/tokens
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:5000 "POST /identity/v2.0/tokens HTTP/1.1" 200 2922
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "HEAD /v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab HTTP/1.1" 404 0
    INFO:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab -I -H "X-Auth-Token: gAAAAABY3h6VYSii2cLGTZvO-12H0TS2lwhq6lQNWnCYImY3LtIiMdhCTxABzr7qAHM7PtLhp6fqQZNEwyr6tM9GvUsYEFm1PQFgsm7f-UNYB2lrJD43Utsc0RMTtla-F9ACYDWasMXfp5I7AXTl0ZW1kGeFNCnIhFqvpY5XsYh97KC3RQbux1c"
    INFO:swiftclient:RESP STATUS: 404 Not Found
    INFO:swiftclient:RESP HEADERS: {u'Date': u'Fri, 31 Mar 2017 09:17:09 GMT', u'Content-Length': u'0', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'tx6b163f143fab42a0b6715-0058de1e95', u'X-Trans-Id': u'tx6b163f143fab42a0b6715-0058de1e95'}
    DEBUG:keystoneclient.auth.identity.v2:Making authentication request to http://137.205.194.8:5000/identity/v2.0/tokens
    DEBUG:keystoneclient.auth.identity.v2:Making authentication request to http://137.205.194.8:5000/identity/v2.0/tokens
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:5000 "POST /identity/v2.0/tokens HTTP/1.1" 200 2922
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:5000 "POST /identity/v2.0/tokens HTTP/1.1" 200 2922
    DEBUG:requests.packages.urllib3.connectionpool:Starting new HTTP connection (1): 137.205.194.8
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test_segments/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950987.875125/38547913/33554432/00000001 HTTP/1.1" 201 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test_segments/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950987.875125/38547913/33554432/00000001 -X PUT -H "Content-Length: 4993481" -H "Content-Type: application/swiftclient-segment" -H "X-Auth-Token: gAAAAABY3h6VJn6xrC8CeVafus1lVZzSnjf5L06sztOp1ZqJJohHJYDRCcqXx1Evl2OAw1TUER4sxQJ7_MOAWZtLDWq6BEQ3OFfEPHWBkHmFIdSnK9jIERoY_LEDQGmp_jK0DJTZYOLrpsNVR56RmpD8mikdSxK5aGDWOuXX9UFffk4zVwPPr4o"
    DEBUG:swiftclient:RESP STATUS: 201 Created
    DEBUG:swiftclient:RESP HEADERS: {u'Content-Length': u'0', u'Last-Modified': u'Fri, 31 Mar 2017 09:17:10 GMT', u'Etag': u'2b4b81733d0a2e4abe89516639627408', u'X-Trans-Id': u'tx31b84da2b4da418dbabfc-0058de1e95', u'Date': u'Fri, 31 Mar 2017 09:17:09 GMT', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'tx31b84da2b4da418dbabfc-0058de1e95'}
    Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab segment 1
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test_segments/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950987.875125/38547913/33554432/00000000 HTTP/1.1" 201 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test_segments/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950987.875125/38547913/33554432/00000000 -X PUT -H "Content-Length: 33554432" -H "Content-Type: application/swiftclient-segment" -H "X-Auth-Token: gAAAAABY3h6V0cPUylT4zgnf7thBET7SRvl-1zZDNqjtc1kIfDcPBfsHZyEYy5izAZdQqsClG9oZkFaZqA59dOypXkCz1KdpgApKnjp7MeZy1wMt44IjbTaK5-j_smU6vfDpMry-5Hrh3TXAeH0A1U23A399qLeBPEAxhmJZlff9VqITHq5y8Lg"
    DEBUG:swiftclient:RESP STATUS: 201 Created
    DEBUG:swiftclient:RESP HEADERS: {u'Content-Length': u'0', u'Last-Modified': u'Fri, 31 Mar 2017 09:17:10 GMT', u'Etag': u'58f06dd588d8ffb3beb46ada6309436b', u'X-Trans-Id': u'txe90416fe10b4489bb01c4-0058de1e95', u'Date': u'Fri, 31 Mar 2017 09:17:09 GMT', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'txe90416fe10b4489bb01c4-0058de1e95'}
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab?multipart-manifest=put HTTP/1.1" 400 347
    INFO:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab?multipart-manifest=put -X PUT -H "x-object-meta-mtime: 1490950987.875125" -H "x-static-large-object: true" -H "Content-Type: " -H "X-Auth-Token: gAAAAABY3h6VYSii2cLGTZvO-12H0TS2lwhq6lQNWnCYImY3LtIiMdhCTxABzr7qAHM7PtLhp6fqQZNEwyr6tM9GvUsYEFm1PQFgsm7f-UNYB2lrJD43Utsc0RMTtla-F9ACYDWasMXfp5I7AXTl0ZW1kGeFNCnIhFqvpY5XsYh97KC3RQbux1c"
    INFO:swiftclient:RESP STATUS: 400 Bad Request
    INFO:swiftclient:RESP HEADERS: {u'Date': u'Fri, 31 Mar 2017 09:17:09 GMT', u'Content-Length': u'347', u'Content-Type': u'text/plain', u'X-Openstack-Request-Id': u'txa7bcf8bea5e54a278a6f0-0058de1e95', u'X-Trans-Id': u'txa7bcf8bea5e54a278a6f0-0058de1e95'}
    INFO:swiftclient:RESP BODY: Errors:
    /slo-test_segments/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950987.875125/38547913/33554432/00000001, 404 Not Found
    /slo-test_segments/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab/slo/1490950987.875125/38547913/33554432/00000000, 404 Not Found
    ERROR:swiftclient.service:Object PUT failed: http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab?multipart-manifest=put 400 Bad Request  [first 60 chars of response] Errors:
    /slo-test_segments/Files/OpenOffice.org%203.3%20%252
    Traceback (most recent call last):
      File "/usr/local/lib/python2.7/dist-packages/swiftclient/service.py", line 1988, in _upload_object_job
        response_dict=mr
      File "/usr/local/lib/python2.7/dist-packages/swiftclient/client.py", line 1824, in put_object
        response_dict=response_dict)
      File "/usr/local/lib/python2.7/dist-packages/swiftclient/client.py", line 1673, in _retry
        service_token=self.service_token, **kwargs)
      File "/usr/local/lib/python2.7/dist-packages/swiftclient/client.py", line 1322, in put_object
        raise ClientException.from_response(resp, 'Object PUT failed', body)
    ClientException: Object PUT failed: http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab?multipart-manifest=put 400 Bad Request  [first 60 chars of response] Errors:
    /slo-test_segments/Files/OpenOffice.org%203.3%20%252
    Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab segment 0
    Object PUT failed: http://137.205.194.8:8080/v1/AUTH_d92af3a8d7f44a1091fe38f863ebb69e/slo-test/Files/OpenOffice.org%203.3%20%2528en-GB%2529%20Installation%20Files/openofficeorg1.cab?multipart-manifest=put 400 Bad Request  [first 60 chars of response] Errors:
    /slo-test_segments/Files/OpenOffice.org%203.3%20%252
